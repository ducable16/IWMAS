package com.iwas.workload.service;

import com.iwas.arrangement.config.AtcProperties;
import com.iwas.arrangement.core.AtcTaskMapper;
import com.iwas.arrangement.core.TardinessArranger;
import com.iwas.arrangement.model.AtcConfig;
import com.iwas.arrangement.model.AtcTask;
import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.notification.NotificationMessages;
import com.iwas.notification.enums.NotificationType;
import com.iwas.notification.service.NotificationService;
import com.iwas.project.entity.Project;
import com.iwas.project.entity.ProjectMember;
import com.iwas.project.repository.ProjectMemberRepository;
import com.iwas.project.repository.ProjectRepository;
import com.iwas.task.entity.Task;
import com.iwas.task.enums.TaskPriority;
import com.iwas.task.repository.TaskRepository;
import com.iwas.user.entity.User;
import com.iwas.user.repository.UserRepository;
import com.iwas.workload.dto.BurnoutLogResponse;
import com.iwas.workload.dto.MemberWorkloadResponse;
import com.iwas.workload.dto.MemberWorkloadResponse.ProjectAllocationItem;
import com.iwas.workload.dto.MemberWorkloadResponse.TaskWorkloadItem;
import com.iwas.workload.dto.WorkloadSnapshotResponse;
import com.iwas.workload.entity.BurnoutLog;
import com.iwas.workload.entity.WorkloadSnapshot;
import com.iwas.workload.enums.WorkloadLevel;
import com.iwas.workload.repository.BurnoutLogRepository;
import com.iwas.workload.repository.WorkloadSnapshotRepository;
import com.iwas.workload.service.ScheduleSimulator.LaneSimulation;
import com.iwas.workload.service.ScheduleSimulator.ScheduledTask;
import com.iwas.workload.service.ScheduleSimulator.TaskSchedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import com.iwas.workload.dto.CandidateWorkloadImpact;
import com.iwas.workload.dto.ProjectScheduleResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Workload v3 — serial scheduling simulation.
 *
 * Each member is modelled as a set of independent project lanes; lane
 * capacity is {@code 8h × allocation%} and never subsidises another lane.
 * Per lane, {@link ScheduleSimulator} forecasts task finish dates and slip
 * risk; the per-member badge is the worst lane badge.
 *
 * A task's outstanding work is {@code reportedRemainingHours} when the member
 * has logged it, otherwise {@code estimatedHours}. The model derives load
 * from this single number plus the schedule — it never re-reads actualHours.
 */
@Service
@RequiredArgsConstructor
public class WorkloadService {

    private final WorkloadSnapshotRepository workloadSnapshotRepository;
    private final BurnoutLogRepository burnoutLogRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;
    private final NotificationService notificationService;
    private final ScheduleSimulator scheduleSimulator;
    private final TardinessArranger tardinessArranger;
    private final AtcProperties atcProperties;

    private static final double DEFAULT_DAILY_HOURS = 8.0;
    private static final BigDecimal TIGHT_THRESHOLD = BigDecimal.valueOf(85);
    private static final BigDecimal AVAILABLE_THRESHOLD = BigDecimal.valueOf(50);

    // ─── task helpers ─────────────────────────────────────────────────────────

    /** Outstanding effort: member-reported remaining if logged, else the estimate. */
    static BigDecimal resolveRemaining(Task t) {
        return t.getReportedRemainingHours() != null
                ? t.getReportedRemainingHours() : t.getEstimatedHours();
    }

    /** A task with no usable estimate and no reported remaining — load is unknown. */
    static boolean isUnestimated(Task t) {
        return t.getReportedRemainingHours() == null
                && (t.getEstimatedHours() == null || t.getEstimatedHours().signum() <= 0);
    }

    /** The member has logged remaining = 0 — work is effectively complete. */
    private static boolean isEffectivelyDone(Task t) {
        return t.getReportedRemainingHours() != null && t.getReportedRemainingHours().signum() <= 0;
    }

    /** Has positive outstanding effort — participates in the simulation. */
    private static boolean isWorkable(Task t) {
        BigDecimal r = resolveRemaining(t);
        return r != null && r.signum() > 0;
    }

    private static boolean isOverdue(Task t, LocalDate today) {
        return t.getDueDate() != null && t.getDueDate().isBefore(today) && !isEffectivelyDone(t);
    }

    private static ScheduledTask toScheduledTask(Task t) {
        return new ScheduledTask(t.getId(), t.getProjectId(), resolveRemaining(t),
                t.getStartDate(), t.getDueDate(), t.getPriority());
    }

    /** Daily lane capacity: null = no allocation row, ZERO = observer, else 8h × alloc%. */
    private static BigDecimal capacityOf(Integer alloc) {
        if (alloc == null) return null;
        if (alloc == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(DEFAULT_DAILY_HOURS * alloc / 100.0)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ─── per-lane computation ─────────────────────────────────────────────────

    private record LaneResult(ProjectAllocationItem item,
                              WorkloadLevel level,
                              BigDecimal nearTermPercent,
                              BigDecimal overallPercent,
                              int overdueCount,
                              int predictedLateCount,
                              int unestimatedCount,
                              List<TaskWorkloadItem> taskItems) {}

    /**
     * Orders a lane's workable tasks. Uses the member's saved executionSeq only
     * when every workable task has one (a complete custom plan); otherwise falls
     * back to the ATC heuristic suggestion. The order is a forecasting hint, not
     * an execution constraint.
     */
    private List<ScheduledTask> orderLane(List<Task> workable, BigDecimal dailyCap) {
        boolean allHaveSeq = !workable.isEmpty()
                && workable.stream().allMatch(t -> t.getExecutionSeq() != null);
        if (allHaveSeq) {
            return workable.stream()
                    .sorted(Comparator.comparingInt(Task::getExecutionSeq))
                    .map(WorkloadService::toScheduledTask)
                    .toList();
        }
        return atcOrder(workable, dailyCap);
    }

    /**
     * System-suggested order via the ATC (Apparent Tardiness Cost) heuristic for
     * total weighted tardiness — a high-performance heuristic, not a proven
     * optimum. Deadlines are measured in the lane's capacity-hours so slack and
     * processing time share one unit.
     */
    private List<ScheduledTask> atcOrder(List<Task> workable, BigDecimal dailyCap) {
        if (workable.isEmpty()) return List.of();
        double cap = dailyCap != null ? dailyCap.doubleValue() : 0.0;
        LocalDate today = LocalDate.now();
        AtcConfig config = AtcConfig.from(atcProperties);
        List<AtcTask> atcTasks = workable.stream()
                .map(t -> AtcTaskMapper.from(t, today, cap))
                .toList();
        Map<Long, Task> byId = workable.stream()
                .collect(Collectors.toMap(Task::getId, t -> t));
        return tardinessArranger.orderTaskIds(atcTasks, config).stream()
                .map(id -> toScheduledTask(byId.get(id)))
                .toList();
    }

    private LaneResult computeLane(User user, Project project, ProjectMember pmRow,
                                   List<Task> laneTasks, LocalDate today) {
        Integer alloc = pmRow != null ? pmRow.getAllocatedEffortPercent() : null;
        boolean managerOnly = pmRow == null && user.getId().equals(project.getManagerId());

        int overdueCount = (int) laneTasks.stream().filter(t -> isOverdue(t, today)).count();
        int unestimatedCount = (int) laneTasks.stream().filter(WorkloadService::isUnestimated).count();

        BigDecimal dailyCap = capacityOf(alloc);
        boolean hasCapacity = dailyCap != null && dailyCap.signum() > 0;

        Map<Long, TaskSchedule> scheduleByTask = new HashMap<>();
        BigDecimal nearPct = null;
        BigDecimal overallPct = null;
        WorkloadLevel level;
        int predictedLate = 0;

        if (!hasCapacity) {
            level = (alloc != null && alloc == 0) ? WorkloadLevel.BLOCKED : WorkloadLevel.UNDEFINED;
        } else {
            List<Task> workable = laneTasks.stream().filter(WorkloadService::isWorkable).toList();
            LaneSimulation sim = scheduleSimulator.simulate(orderLane(workable, dailyCap), dailyCap, today);
            for (TaskSchedule ts : sim.schedules()) scheduleByTask.put(ts.taskId(), ts);
            nearPct = sim.nearTermPercent();
            overallPct = sim.overallPercent();
            // Slip predictions that are NOT already overdue (overdue is its own badge).
            predictedLate = (int) sim.schedules().stream()
                    .filter(TaskSchedule::willSlip)
                    .filter(ts -> {
                        LocalDate due = dueDateOf(ts.taskId(), laneTasks);
                        return due != null && !due.isBefore(today);
                    })
                    .count();
            level = laneBadge(overdueCount, predictedLate, nearPct);
        }

        List<TaskWorkloadItem> taskItems = laneTasks.stream()
                .map(t -> toTaskItem(t, today, scheduleByTask.get(t.getId())))
                .sorted(Comparator.comparing(
                        (TaskWorkloadItem i) -> i.getDueDate() == null ? LocalDate.MAX : i.getDueDate()))
                .toList();

        ProjectAllocationItem item = ProjectAllocationItem.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .allocatedEffortPercent(alloc)
                .dailyCapacityHours(dailyCap)
                .workloadLevel(level)
                .nearTermPercent(nearPct)
                .overallPercent(overallPct)
                .predictedLateTaskCount(predictedLate)
                .build();

        return new LaneResult(item, level, nearPct, overallPct,
                overdueCount, predictedLate, unestimatedCount, taskItems);
    }

    private static LocalDate dueDateOf(Long taskId, List<Task> tasks) {
        return tasks.stream()
                .filter(t -> t.getId().equals(taskId))
                .map(Task::getDueDate)
                .findFirst()
                .orElse(null);
    }

    private static WorkloadLevel laneBadge(int overdueCount, int predictedLate, BigDecimal nearPct) {
        if (overdueCount > 0) return WorkloadLevel.OVERDUE;
        if (predictedLate > 0) return WorkloadLevel.WILL_SLIP;
        if (nearPct.compareTo(TIGHT_THRESHOLD) >= 0) return WorkloadLevel.TIGHT;
        if (nearPct.compareTo(AVAILABLE_THRESHOLD) < 0) return WorkloadLevel.AVAILABLE;
        return WorkloadLevel.HEALTHY;
    }

    private TaskWorkloadItem toTaskItem(Task t, LocalDate today, TaskSchedule sched) {
        boolean unestimated = isUnestimated(t);
        return TaskWorkloadItem.builder()
                .taskId(t.getId())
                .projectId(t.getProjectId())
                .title(t.getTitle())
                .status(t.getStatus())
                .priority(t.getPriority())
                .startDate(t.getStartDate())
                .dueDate(t.getDueDate())
                .remainingHours(unestimated ? null : resolveRemaining(t))
                .executionSeq(t.getExecutionSeq())
                .projectedStartDate(sched != null ? sched.projectedStart() : null)
                .projectedFinishDate(sched != null ? sched.projectedFinish() : null)
                .willSlip(sched != null && sched.willSlip())
                .lateByWorkdays(sched != null ? sched.lateByWorkdays() : 0)
                .overdue(isOverdue(t, today))
                .unestimated(unestimated)
                .build();
    }

    // ─── per-member aggregation ───────────────────────────────────────────────

    private record MemberWorkload(WorkloadLevel level,
                                  BigDecimal nearTermPercent,
                                  BigDecimal overallPercent,
                                  int activeTaskCount,
                                  int overdueTaskCount,
                                  int predictedLateTaskCount,
                                  int unestimatedTaskCount,
                                  int projectCount,
                                  List<ProjectAllocationItem> projectItems,
                                  List<TaskWorkloadItem> taskItems) {}

    private static int severityRank(WorkloadLevel l) {
        return switch (l) {
            case OVERDUE -> 0;
            case WILL_SLIP -> 1;
            case TIGHT -> 2;
            case HEALTHY -> 3;
            case AVAILABLE -> 4;
            case BLOCKED -> 5;
            case UNDEFINED -> 6;
        };
    }

    private MemberWorkload computeMemberWorkload(User user, LocalDate today) {
        Long userId = user.getId();
        List<Task> activeTasks = taskRepository.findActiveTasksByAssigneeId(userId);
        Map<Long, List<Task>> tasksByProject = activeTasks.stream()
                .collect(Collectors.groupingBy(Task::getProjectId));

        List<ProjectMember> memberships = projectMemberRepository.findActiveProjectsByUserId(userId);
        Map<Long, ProjectMember> pmByProject = memberships.stream()
                .collect(Collectors.toMap(ProjectMember::getProjectId, m -> m, (a, b) -> a));

        Set<Long> projectIds = new LinkedHashSet<>();
        memberships.forEach(m -> projectIds.add(m.getProjectId()));
        projectRepository.findByManagerId(userId).forEach(p -> projectIds.add(p.getId()));
        projectIds.addAll(tasksByProject.keySet());

        Map<Long, Project> projectMap = projectRepository.findAllById(projectIds).stream()
                .collect(Collectors.toMap(Project::getId, p -> p));

        List<ProjectAllocationItem> projectItems = new ArrayList<>();
        List<TaskWorkloadItem> taskItems = new ArrayList<>();
        WorkloadLevel aggregate = null;
        BigDecimal nearMax = null;
        BigDecimal overallMax = null;
        int overdueTotal = 0;
        int lateTotal = 0;
        int unestimatedTotal = 0;

        for (Long pid : projectIds) {
            Project project = projectMap.get(pid);
            if (project == null) continue;
            LaneResult lane = computeLane(user, project, pmByProject.get(pid),
                    tasksByProject.getOrDefault(pid, List.of()), today);
            projectItems.add(lane.item());
            taskItems.addAll(lane.taskItems());
            overdueTotal += lane.overdueCount();
            lateTotal += lane.predictedLateCount();
            unestimatedTotal += lane.unestimatedCount();
            if (aggregate == null || severityRank(lane.level()) < severityRank(aggregate)) {
                aggregate = lane.level();
            }
            nearMax = maxNullable(nearMax, lane.nearTermPercent());
            overallMax = maxNullable(overallMax, lane.overallPercent());
        }

        return new MemberWorkload(
                aggregate != null ? aggregate : WorkloadLevel.UNDEFINED,
                nearMax != null ? nearMax : BigDecimal.ZERO,
                overallMax != null ? overallMax : BigDecimal.ZERO,
                activeTasks.size(), overdueTotal, lateTotal, unestimatedTotal,
                memberships.size(), projectItems, taskItems);
    }

    private static BigDecimal maxNullable(BigDecimal a, BigDecimal b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.max(b);
    }

    // ─── public read endpoints ────────────────────────────────────────────────

    public List<WorkloadSnapshotResponse> getTeamWorkload(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return enrichSnapshots(workloadSnapshotRepository.findBySnapshotDate(targetDate));
    }

    public List<WorkloadSnapshotResponse> getUserWorkloadHistory(Long userId) {
        return enrichSnapshots(workloadSnapshotRepository.findByUserIdOrderByDateDesc(userId));
    }

    public List<BurnoutLogResponse> getBurnoutLogs() {
        return enrichBurnoutLogs(burnoutLogRepository.findAll());
    }

    public List<BurnoutLogResponse> getUserBurnoutHistory(Long userId) {
        return enrichBurnoutLogs(burnoutLogRepository.findByUserIdOrderByEvaluatedAtDesc(userId));
    }

    /** Real-time workload for all active participants of a project. */
    public List<MemberWorkloadResponse> getProjectMembersWorkload(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        LocalDate today = LocalDate.now();

        LinkedHashSet<Long> userIds = new LinkedHashSet<>();
        if (project.getManagerId() != null) userIds.add(project.getManagerId());
        projectMemberRepository.findActiveMembersByProjectId(projectId)
                .forEach(m -> userIds.add(m.getUserId()));

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return userIds.stream()
                .filter(uid -> userMap.containsKey(uid)
                        && !Boolean.TRUE.equals(userMap.get(uid).getIsDeleted()))
                .map(uid -> {
                    User user = userMap.get(uid);
                    MemberWorkload mw = computeMemberWorkload(user, today);
                    // Project view: keep the aggregate badge, show only this project's lane.
                    List<ProjectAllocationItem> thisProject = mw.projectItems().stream()
                            .filter(i -> projectId.equals(i.getProjectId()))
                            .toList();
                    return toMemberResponse(user, mw, today, thisProject, null);
                })
                .toList();
    }

    /** Real-time workload for a single user — aggregate + per-project lanes + task list. */
    public MemberWorkloadResponse getUserWorkloadRealtime(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        LocalDate today = LocalDate.now();
        MemberWorkload mw = computeMemberWorkload(user, today);
        return toMemberResponse(user, mw, today, mw.projectItems(), mw.taskItems());
    }

    @Transactional
    public WorkloadSnapshotResponse takeSnapshot(Long userId) {
        LocalDate today = LocalDate.now();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        MemberWorkload mw = computeMemberWorkload(user, today);

        WorkloadSnapshot snapshot = workloadSnapshotRepository
                .findByUserIdAndSnapshotDate(userId, today)
                .orElseGet(WorkloadSnapshot::new);
        snapshot.setUserId(userId);
        snapshot.setSnapshotDate(today);
        snapshot.setProjectCount(mw.projectCount());
        snapshot.setActiveTaskCount(mw.activeTaskCount());
        snapshot.setOverdueTaskCount(mw.overdueTaskCount());
        snapshot.setPredictedLateTaskCount(mw.predictedLateTaskCount());
        snapshot.setUnestimatedTaskCount(mw.unestimatedTaskCount());
        snapshot.setNearTermPercent(mw.nearTermPercent());
        snapshot.setOverallPercent(mw.overallPercent());
        snapshot.setWorkloadLevel(mw.level());

        WorkloadSnapshot saved = workloadSnapshotRepository.save(snapshot);

        if (mw.level() == WorkloadLevel.OVERDUE || mw.level() == WorkloadLevel.WILL_SLIP) {
            notificationService.send(
                    userId, NotificationType.OVERLOAD_WARNING,
                    NotificationMessages.overloadWarning(mw.overallPercent().toPlainString()),
                    "WORKLOAD", saved.getId());
        }

        return toSnapshotResponse(saved, user.getFullName());
    }

    // ─── what-if scheduling (single member, single project lane) ──────────────

    private record ScheduleContext(User user, Project project, ProjectMember pmRow,
                                   Integer alloc, BigDecimal dailyCap,
                                   List<Task> laneTasks, List<Task> workable) {}

    private ScheduleContext loadScheduleContext(Long userId, Long projectId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        ProjectMember pmRow = projectMemberRepository
                .findActiveMemberByProjectIdAndUserId(projectId, userId).orElse(null);
        boolean managerOnly = pmRow == null && userId.equals(project.getManagerId());
        if (pmRow == null && !managerOnly) throw new AppException(ErrorCode.FORBIDDEN);

        List<Task> laneTasks = taskRepository.findActiveTasksByProjectIdAndAssigneeId(projectId, userId);
        List<Task> workable = laneTasks.stream().filter(WorkloadService::isWorkable).toList();

        Integer alloc = pmRow != null ? pmRow.getAllocatedEffortPercent() : null;
        return new ScheduleContext(user, project, pmRow, alloc, capacityOf(alloc), laneTasks, workable);
    }

    private List<ScheduledTask> applyExplicitOrder(List<Task> workable, List<Long> orderedTaskIds) {
        Map<Long, Task> byId = workable.stream()
                .collect(Collectors.toMap(Task::getId, t -> t));
        List<Long> ids = orderedTaskIds != null ? orderedTaskIds : List.of();
        if (ids.size() != byId.size() || !new HashSet<>(ids).equals(byId.keySet())) {
            throw new AppException(ErrorCode.INVALID_INPUT,
                    "Ordered task ids must be exactly the lane's schedulable tasks");
        }
        return ids.stream().map(id -> toScheduledTask(byId.get(id))).toList();
    }

    private ProjectScheduleResponse buildProjectSchedule(ScheduleContext ctx,
                                                         List<ScheduledTask> ordered,
                                                         boolean savedOrder, LocalDate today) {
        int overdueCount = (int) ctx.laneTasks().stream().filter(t -> isOverdue(t, today)).count();

        Map<Long, TaskSchedule> scheduleByTask = new HashMap<>();
        WorkloadLevel level;
        BigDecimal nearPct = null;
        BigDecimal overallPct = null;
        int predictedLate = 0;

        if (ctx.dailyCap() == null) {
            level = WorkloadLevel.UNDEFINED;
        } else if (ctx.dailyCap().signum() <= 0) {
            level = WorkloadLevel.BLOCKED;
        } else {
            LaneSimulation sim = scheduleSimulator.simulate(ordered, ctx.dailyCap(), today);
            for (TaskSchedule ts : sim.schedules()) scheduleByTask.put(ts.taskId(), ts);
            nearPct = sim.nearTermPercent();
            overallPct = sim.overallPercent();
            predictedLate = (int) sim.schedules().stream()
                    .filter(TaskSchedule::willSlip)
                    .filter(ts -> {
                        LocalDate due = dueDateOf(ts.taskId(), ctx.laneTasks());
                        return due != null && !due.isBefore(today);
                    })
                    .count();
            level = laneBadge(overdueCount, predictedLate, nearPct);
        }

        Map<Long, TaskWorkloadItem> itemById = new HashMap<>();
        for (Task t : ctx.laneTasks()) {
            itemById.put(t.getId(), toTaskItem(t, today, scheduleByTask.get(t.getId())));
        }
        // Schedulable tasks in execution order, then the rest (done / unestimated).
        List<TaskWorkloadItem> items = new ArrayList<>();
        Set<Long> placed = new HashSet<>();
        for (ScheduledTask st : ordered) {
            items.add(itemById.get(st.taskId()));
            placed.add(st.taskId());
        }
        for (Task t : ctx.laneTasks()) {
            if (!placed.contains(t.getId())) items.add(itemById.get(t.getId()));
        }

        return ProjectScheduleResponse.builder()
                .projectId(ctx.project().getId())
                .projectName(ctx.project().getName())
                .allocatedEffortPercent(ctx.alloc())
                .dailyCapacityHours(ctx.dailyCap())
                .workloadLevel(level)
                .nearTermPercent(nearPct)
                .overallPercent(overallPct)
                .predictedLateTaskCount(predictedLate)
                .savedOrder(savedOrder)
                .tasks(items)
                .build();
    }

    /** Current saved schedule for the member's lane (saved executionSeq, else EDD). */
    public ProjectScheduleResponse getMySchedule(Long userId, Long projectId) {
        LocalDate today = LocalDate.now();
        ScheduleContext ctx = loadScheduleContext(userId, projectId);
        boolean saved = !ctx.workable().isEmpty()
                && ctx.workable().stream().allMatch(t -> t.getExecutionSeq() != null);
        return buildProjectSchedule(ctx, orderLane(ctx.workable(), ctx.dailyCap()), saved, today);
    }

    /** ATC heuristic order suggestion — a preview, not persisted. */
    public ProjectScheduleResponse suggestSchedule(Long userId, Long projectId) {
        LocalDate today = LocalDate.now();
        ScheduleContext ctx = loadScheduleContext(userId, projectId);
        return buildProjectSchedule(ctx, atcOrder(ctx.workable(), ctx.dailyCap()), false, today);
    }

    /** Simulates a member-proposed order without persisting it. */
    public ProjectScheduleResponse previewSchedule(Long userId, Long projectId,
                                                   List<Long> orderedTaskIds) {
        LocalDate today = LocalDate.now();
        ScheduleContext ctx = loadScheduleContext(userId, projectId);
        return buildProjectSchedule(ctx, applyExplicitOrder(ctx.workable(), orderedTaskIds),
                false, today);
    }

    /** Persists the member-proposed order as executionSeq, then returns the simulation. */
    @Transactional
    public ProjectScheduleResponse saveSchedule(Long userId, Long projectId,
                                                List<Long> orderedTaskIds) {
        LocalDate today = LocalDate.now();
        ScheduleContext ctx = loadScheduleContext(userId, projectId);
        List<ScheduledTask> ordered = applyExplicitOrder(ctx.workable(), orderedTaskIds);

        Map<Long, Task> byId = ctx.workable().stream()
                .collect(Collectors.toMap(Task::getId, t -> t));
        int seq = 1;
        for (ScheduledTask st : ordered) {
            byId.get(st.taskId()).setExecutionSeq(seq++);
        }
        taskRepository.saveAll(byId.values());

        return buildProjectSchedule(ctx, ordered, true, today);
    }

    // ─── recommender hook ─────────────────────────────────────────────────────

    private static final long CANDIDATE_TASK_ID = -1L;

    /**
     * What-if for the assignee recommender: the workload impact of giving the
     * candidate one hypothetical task. Re-runs the lane simulation with the
     * task appended in EDD order; persists nothing. Provided for the
     * recommender to read availability — not itself wired into TOPSIS scoring.
     */
    public CandidateWorkloadImpact simulateWithCandidateTask(
            Long userId, Long projectId, BigDecimal estimatedHours,
            LocalDate startDate, LocalDate dueDate, TaskPriority priority) {
        LocalDate today = LocalDate.now();
        projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));
        if (!userRepository.existsById(userId)) throw new AppException(ErrorCode.USER_NOT_FOUND);

        ProjectMember pmRow = projectMemberRepository
                .findActiveMemberByProjectIdAndUserId(projectId, userId).orElse(null);
        BigDecimal dailyCap = capacityOf(pmRow != null ? pmRow.getAllocatedEffortPercent() : null);

        List<Task> laneTasks = taskRepository.findActiveTasksByProjectIdAndAssigneeId(projectId, userId);
        int overdueCount = (int) laneTasks.stream().filter(t -> isOverdue(t, today)).count();

        CandidateWorkloadImpact.CandidateWorkloadImpactBuilder b = CandidateWorkloadImpact.builder()
                .userId(userId).projectId(projectId);

        if (dailyCap == null || dailyCap.signum() <= 0) {
            WorkloadLevel lvl = dailyCap == null ? WorkloadLevel.UNDEFINED : WorkloadLevel.BLOCKED;
            return b.levelBefore(lvl).levelAfter(lvl)
                    .candidateTaskWillSlip(true).introducesNewSlip(false)
                    .predictedLateTaskCountAfter(0).build();
        }

        List<ScheduledTask> existing = laneTasks.stream()
                .filter(WorkloadService::isWorkable).map(WorkloadService::toScheduledTask).toList();
        ScheduledTask candidate = new ScheduledTask(CANDIDATE_TASK_ID, projectId,
                estimatedHours, startDate, dueDate, priority);

        Map<Long, LocalDate> dueMap = new HashMap<>();
        for (Task t : laneTasks) dueMap.put(t.getId(), t.getDueDate());
        dueMap.put(CANDIDATE_TASK_ID, dueDate);

        LaneSimulation before = scheduleSimulator.simulate(
                ScheduleSimulator.eddOrder(existing), dailyCap, today);
        List<ScheduledTask> withCandidate = new ArrayList<>(existing);
        withCandidate.add(candidate);
        LaneSimulation after = scheduleSimulator.simulate(
                ScheduleSimulator.eddOrder(withCandidate), dailyCap, today);

        int lateBefore = futureSlipCount(before, dueMap, today);
        int lateAfter = futureSlipCount(after, dueMap, today);
        boolean candidateSlips = after.schedules().stream()
                .anyMatch(ts -> ts.taskId().equals(CANDIDATE_TASK_ID) && ts.willSlip());

        return b
                .levelBefore(laneBadge(overdueCount, lateBefore, before.nearTermPercent()))
                .levelAfter(laneBadge(overdueCount, lateAfter, after.nearTermPercent()))
                .overallPercentBefore(before.overallPercent())
                .overallPercentAfter(after.overallPercent())
                .nearTermPercentAfter(after.nearTermPercent())
                .predictedLateTaskCountAfter(lateAfter)
                .candidateTaskWillSlip(candidateSlips)
                .introducesNewSlip(lateAfter > lateBefore)
                .build();
    }

    private static int futureSlipCount(LaneSimulation sim, Map<Long, LocalDate> dueMap,
                                       LocalDate today) {
        return (int) sim.schedules().stream()
                .filter(TaskSchedule::willSlip)
                .filter(ts -> {
                    LocalDate due = dueMap.get(ts.taskId());
                    return due != null && !due.isBefore(today);
                })
                .count();
    }

    // ─── private mapping ──────────────────────────────────────────────────────

    private MemberWorkloadResponse toMemberResponse(User user, MemberWorkload mw, LocalDate today,
                                                    List<ProjectAllocationItem> projectItems,
                                                    List<TaskWorkloadItem> taskItems) {
        return MemberWorkloadResponse.builder()
                .userId(user.getId())
                .userFullName(user.getFullName())
                .weekStart(today)
                .weekEnd(ScheduleSimulator.addWorkdays(today, ScheduleSimulator.NEAR_TERM_WORKDAYS - 1))
                .workloadLevel(mw.level())
                .nearTermPercent(mw.nearTermPercent())
                .overallPercent(mw.overallPercent())
                .activeTaskCount(mw.activeTaskCount())
                .overdueTaskCount(mw.overdueTaskCount())
                .predictedLateTaskCount(mw.predictedLateTaskCount())
                .unestimatedTaskCount(mw.unestimatedTaskCount())
                .projectAllocations(projectItems)
                .tasks(taskItems)
                .build();
    }

    private List<WorkloadSnapshotResponse> enrichSnapshots(List<WorkloadSnapshot> snapshots) {
        if (snapshots.isEmpty()) return List.of();
        Map<Long, String> names = userRepository.findAllById(
                snapshots.stream().map(WorkloadSnapshot::getUserId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, User::getFullName));

        return snapshots.stream()
                .map(ws -> toSnapshotResponse(ws, names.get(ws.getUserId())))
                .toList();
    }

    private List<BurnoutLogResponse> enrichBurnoutLogs(List<BurnoutLog> logs) {
        if (logs.isEmpty()) return List.of();
        Map<Long, String> names = userRepository.findAllById(
                logs.stream().map(BurnoutLog::getUserId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, User::getFullName));

        return logs.stream()
                .map(bl -> toBurnoutResponse(bl, names.get(bl.getUserId())))
                .toList();
    }

    private WorkloadSnapshotResponse toSnapshotResponse(WorkloadSnapshot ws, String name) {
        return WorkloadSnapshotResponse.builder()
                .id(ws.getId())
                .userId(ws.getUserId())
                .userFullName(name)
                .snapshotDate(ws.getSnapshotDate())
                .projectCount(ws.getProjectCount())
                .activeTaskCount(ws.getActiveTaskCount())
                .overdueTaskCount(ws.getOverdueTaskCount())
                .predictedLateTaskCount(ws.getPredictedLateTaskCount())
                .unestimatedTaskCount(ws.getUnestimatedTaskCount())
                .nearTermPercent(ws.getNearTermPercent())
                .overallPercent(ws.getOverallPercent())
                .workloadLevel(ws.getWorkloadLevel())
                .build();
    }

    private BurnoutLogResponse toBurnoutResponse(BurnoutLog bl, String name) {
        return BurnoutLogResponse.builder()
                .id(bl.getId())
                .userId(bl.getUserId())
                .userFullName(name)
                .evaluatedAt(bl.getEvaluatedAt())
                .riskScore(bl.getRiskScore())
                .riskLevel(bl.getRiskLevel())
                .overdueTaskCount(bl.getOverdueTaskCount())
                .capacityUsedAvg(bl.getCapacityUsedAvg())
                .isAlertSent(bl.getIsAlertSent())
                .note(bl.getNote())
                .build();
    }
}
