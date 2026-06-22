package com.iwas.workload.service;

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
import com.iwas.workload.dto.MemberWorkloadResponse;
import com.iwas.workload.dto.MemberWorkloadResponse.ProjectAllocationItem;
import com.iwas.workload.dto.MemberWorkloadResponse.TaskWorkloadItem;
import com.iwas.workload.enums.LoadLevel;
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
 * A task's outstanding work is {@code estimatedHours}. The model derives load
 * from this single number plus the schedule.
 */
@Service
@RequiredArgsConstructor
public class WorkloadService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;
    private final NotificationService notificationService;
    private final ScheduleSimulator scheduleSimulator;
    private final TardinessArranger tardinessArranger;

    private static final double DEFAULT_DAILY_HOURS = 8.0;
    // Dashboard "Workload" axis — backlog depth thresholds, in workdays of queued work.
    private static final BigDecimal LOAD_BUSY_DAYS = BigDecimal.valueOf(5);        // ≤5 workdays → AVAILABLE
    private static final BigDecimal LOAD_OVERLOADED_DAYS = BigDecimal.valueOf(10);  // ≤10 → BUSY, else OVERLOADED

    // ─── task helpers ─────────────────────────────────────────────────────────

    /** Outstanding effort: the task's estimate. */
    static BigDecimal resolveRemaining(Task t) {
        return t.getEstimatedHours();
    }

    /** A task with no usable estimate — load is unknown. */
    static boolean isUnestimated(Task t) {
        return t.getEstimatedHours() == null || t.getEstimatedHours().signum() <= 0;
    }

    /** Has positive outstanding effort — participates in the simulation. */
    private static boolean isWorkable(Task t) {
        BigDecimal est = t.getEstimatedHours();
        return est != null && est.signum() > 0;
    }

    private static boolean isOverdue(Task t, LocalDate today) {
        return t.getDueDate() != null && t.getDueDate().isBefore(today);
    }

    private static ScheduledTask toScheduledTask(Task t) {
        return new ScheduledTask(t.getId(), t.getProjectId(), t.getEstimatedHours(),
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

    private record LaneLoad(ProjectAllocationItem item,
                            LoadLevel loadLevel,
                            BigDecimal backlogDays,
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
        AtcConfig config = AtcConfig.getDefault();
        List<AtcTask> atcTasks = workable.stream()
                .map(t -> AtcTaskMapper.from(t, today, cap))
                .toList();
        Map<Long, Task> byId = workable.stream()
                .collect(Collectors.toMap(Task::getId, t -> t));
        return tardinessArranger.orderTaskIds(atcTasks, config).stream()
                .map(id -> toScheduledTask(byId.get(id)))
                .toList();
    }

    private LaneLoad computeLaneLoad(Project project, ProjectMember prjMem, List<Task> laneTasks, LocalDate today) {
        Integer allocation = prjMem != null ? prjMem.getAllocatedEffortPercent() : null;
        BigDecimal dailyCapacity = capacityOf(allocation);
        boolean hasCapacity = dailyCapacity != null && dailyCapacity.signum() > 0;

        List<Task> workable = laneTasks.stream().filter(WorkloadService::isWorkable).toList();
        // Workload axis: backlog volume — deadline-agnostic, order-independent.
        BigDecimal backlogHours = workable.stream()
                .map(WorkloadService::resolveRemaining)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int overdueCount = (int) laneTasks.stream().filter(t -> isOverdue(t, today)).count();
        int unestimatedCount = (int) laneTasks.stream().filter(WorkloadService::isUnestimated).count();

        Map<Long, TaskSchedule> scheduleByTask = new HashMap<>();
        BigDecimal backlogDays = null;
        int predictedLate = 0;
        LoadLevel loadLevel;

        if (!hasCapacity) {
            loadLevel = (allocation != null && allocation == 0) ? LoadLevel.BLOCKED : LoadLevel.UNDEFINED;
        } else {
            backlogDays = backlogHours.divide(dailyCapacity, 2, RoundingMode.HALF_UP);
            loadLevel = loadLevelFor(backlogDays);
            // Risk axis: simulate to find slips. startDate no longer gates, so every workable
            // task is scheduled from today in ATC (or the member's saved) order.
            LaneSimulation sim = scheduleSimulator.simulate(orderLane(workable, dailyCapacity), dailyCapacity, today);
            for (TaskSchedule ts : sim.schedules()) scheduleByTask.put(ts.taskId(), ts);
            predictedLate = (int) sim.schedules().stream()
                    .filter(ts -> isFutureSlip(ts, laneTasks, today))
                    .count();
        }

        List<TaskWorkloadItem> taskItems = laneTasks.stream()
                .map(t -> toTaskItem(t, today, scheduleByTask.get(t.getId())))
                .sorted(Comparator.comparing(
                        (TaskWorkloadItem i) -> i.getDueDate() == null ? LocalDate.MAX : i.getDueDate()))
                .toList();

        ProjectAllocationItem item = ProjectAllocationItem.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .allocatedEffortPercent(allocation)
                .dailyCapacityHours(dailyCapacity)
                .backlogHours(backlogHours)
                .backlogDays(backlogDays)
                .loadLevel(loadLevel)
                .overdueCount(overdueCount)
                .predictedLateTaskCount(predictedLate)
                .build();

        return new LaneLoad(item, loadLevel, backlogDays,
                overdueCount, predictedLate, unestimatedCount, taskItems);
    }

    /** Maps backlog depth (workdays of queued work) to a load badge. */
    private static LoadLevel loadLevelFor(BigDecimal backlogDays) {
        if (backlogDays.compareTo(LOAD_BUSY_DAYS) <= 0) return LoadLevel.AVAILABLE;
        if (backlogDays.compareTo(LOAD_OVERLOADED_DAYS) <= 0) return LoadLevel.BUSY;
        return LoadLevel.OVERLOADED;
    }

    private static LocalDate dueDateOf(Long taskId, List<Task> tasks) {
        return tasks.stream()
                .filter(t -> t.getId().equals(taskId))
                .map(Task::getDueDate)
                .findFirst()
                .orElse(null);
    }

    /**
     * A future slip: the simulation predicts the task misses its deadline, and that deadline
     * is today or later. Already-overdue tasks (due date in the past) are excluded here — they
     * are counted by {@code overdueCount} instead, so we don't double-count them as predicted-late.
     */
    private static boolean isFutureSlip(TaskSchedule ts, List<Task> laneTasks, LocalDate today) {
        if (!ts.willSlip()) return false;
        LocalDate due = dueDateOf(ts.taskId(), laneTasks);
        return due != null && !due.isBefore(today);
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
                .remainingHours(unestimated ? null : t.getEstimatedHours())
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

    private record MemberLoad(LoadLevel loadLevel,
                              BigDecimal worstBacklogDays,
                              int atRiskCount,
                              int activeTaskCount,
                              int overdueTaskCount,
                              int predictedLateTaskCount,
                              int unestimatedTaskCount,
                              List<ProjectAllocationItem> projectItems,
                              List<TaskWorkloadItem> taskItems) {}

    /** Severity for picking the member's worst lane (lower = worse). */
    private static int loadRank(LoadLevel l) {
        return switch (l) {
            case OVERLOADED -> 0;
            case BUSY -> 1;
            case AVAILABLE -> 2;
            case BLOCKED -> 3;
            case UNDEFINED -> 4;
        };
    }

    private static LoadLevel worse(LoadLevel a, LoadLevel b) {
        if (a == null) return b;
        if (b == null) return a;
        return loadRank(a) <= loadRank(b) ? a : b;
    }

    private MemberLoad computeMemberLoad(User user, LocalDate today) {
        Long userId = user.getId();
        List<Task> activeTasks = taskRepository.findActiveTasksByAssigneeId(userId);
        Map<Long, List<Task>> tasksByProject = activeTasks.stream()
                .collect(Collectors.groupingBy(Task::getProjectId));

        List<ProjectMember> memberships = projectMemberRepository.findActiveProjectsByUserId(userId);

        // map nhung membership
        Map<Long, ProjectMember> pmByProject = new HashMap<>();

        // list project id
        List<Long> projectIds = new ArrayList<>();
        for (ProjectMember m : memberships) {
            pmByProject.put(m.getProjectId(), m);
            projectIds.add(m.getProjectId());
        }

        // map nhung project tham gia
        Map<Long, Project> projectMap = new HashMap<>();
        for (Project p : projectRepository.findAllById(projectIds)) {
            projectMap.put(p.getId(), p);
        }

        List<ProjectAllocationItem> projectItems = new ArrayList<>();
        List<TaskWorkloadItem> taskItems = new ArrayList<>();
        LoadLevel worstLevel = null;
        BigDecimal worstDays = null;
        int overdueTotal = 0;
        int lateTotal = 0;
        int unestimatedTotal = 0;

        for (Long pid : projectIds) {
            Project project = projectMap.get(pid);
            LaneLoad lane = computeLaneLoad(project, pmByProject.get(pid), tasksByProject.getOrDefault(pid, List.of()), today);

            projectItems.add(lane.item());
            taskItems.addAll(lane.taskItems());
            overdueTotal += lane.overdueCount();
            lateTotal += lane.predictedLateCount();
            unestimatedTotal += lane.unestimatedCount();
            worstLevel = worse(worstLevel, lane.loadLevel());
            worstDays = maxNullable(worstDays, lane.backlogDays());
        }

        return new MemberLoad(
                worstLevel != null ? worstLevel : LoadLevel.UNDEFINED,
                worstDays,
                overdueTotal + lateTotal,
                activeTasks.size(), overdueTotal, lateTotal, unestimatedTotal,
                projectItems, taskItems);
    }

    private static BigDecimal maxNullable(BigDecimal a, BigDecimal b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.max(b);
    }

    // ─── public read endpoints ────────────────────────────────────────────────

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
                    MemberLoad ml = computeMemberLoad(user, today);
                    // Project view: keep the aggregate axes, show only this project's lane.
                    List<ProjectAllocationItem> thisProject = ml.projectItems().stream()
                            .filter(i -> projectId.equals(i.getProjectId()))
                            .toList();
                    return toMemberResponse(user, ml, thisProject, null);
                })
                .toList();
    }

    /** Real-time load for a single user — aggregate + per-project lanes + task list. */
    public MemberWorkloadResponse getUserWorkloadRealtime(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        LocalDate today = LocalDate.now();
        MemberLoad ml = computeMemberLoad(user, today);
        return toMemberResponse(user, ml, ml.projectItems(), ml.taskItems());
    }

    /**
     * Real-time workload of every member across the projects the given manager manages
     * ({@code managerId = managerId}), deduplicated — a member in several of the manager's
     * projects appears exactly once. The manager themselves is excluded (this is their team).
     *
     * <p>Collapses the FE's per-member fan-out of {@code GET /workload/users/{id}/realtime}
     * into a single request. Each element matches {@link #getUserWorkloadRealtime} — aggregate
     * badge, per-project lanes, and the task breakdown — computed over each member's full set of
     * projects (not only the manager's), so it reflects their true total load.
     */
    public List<MemberWorkloadResponse> getManagedMembersWorkload(Long managerId) {
        List<Long> projectIds = projectRepository.findByManagerId(managerId)
                .stream().map(Project::getId).toList();
        if (projectIds.isEmpty()) return List.of();

        LocalDate today = LocalDate.now();
        List<User> members = getActiveTeamMembers(projectIds, managerId);

        List<MemberWorkloadResponse> result = new ArrayList<>();
        for (User user : members) {
            MemberLoad ml = computeMemberLoad(user, today);
            result.add(toMemberResponse(user, ml, ml.projectItems(), ml.taskItems()));
        }
        return result;
    }

    private List<User> getActiveTeamMembers(List<Long> projectIds, Long excludeUserId) {
        LinkedHashSet<Long> userIds = new LinkedHashSet<>();
        projectMemberRepository.findActiveMembersByProjectIdIn(projectIds)
                .forEach(m -> userIds.add(m.getUserId()));
        userIds.remove(excludeUserId);

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return userIds.stream()
                .map(userMap::get)
                .filter(u -> u != null && !Boolean.TRUE.equals(u.getIsDeleted()))
                .toList();
    }

    /**
     * Daily deadline-risk check for one member: runs the simulation and, when any task is
     * overdue or predicted to slip ({@code atRiskCount > 0}), sends an OVERLOAD_WARNING.
     * Computes live — nothing is persisted.
     */
    @Transactional
    public void evaluateOverload(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        MemberLoad ml = computeMemberLoad(user, LocalDate.now());

        if (ml.atRiskCount() > 0) {
            notificationService.send(
                    userId, NotificationType.OVERLOAD_WARNING,
                    NotificationMessages.overloadWarning(ml.atRiskCount()),
                    "WORKLOAD", userId);
        }
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

        BigDecimal dailyCap = ctx.dailyCap();
        BigDecimal backlogHours = ctx.workable().stream()
                .map(WorkloadService::resolveRemaining)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, TaskSchedule> scheduleByTask = new HashMap<>();
        BigDecimal backlogDays = null;
        LoadLevel loadLevel;
        int predictedLate = 0;

        if (dailyCap == null) {
            loadLevel = LoadLevel.UNDEFINED;
        } else if (dailyCap.signum() <= 0) {
            loadLevel = LoadLevel.BLOCKED;
        } else {
            backlogDays = backlogHours.divide(dailyCap, 2, RoundingMode.HALF_UP);
            loadLevel = loadLevelFor(backlogDays);
            LaneSimulation sim = scheduleSimulator.simulate(ordered, dailyCap, today);
            for (TaskSchedule ts : sim.schedules()) scheduleByTask.put(ts.taskId(), ts);
            predictedLate = (int) sim.schedules().stream()
                    .filter(ts -> isFutureSlip(ts, ctx.laneTasks(), today))
                    .count();
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
                .dailyCapacityHours(dailyCap)
                .backlogHours(backlogHours)
                .backlogDays(backlogDays)
                .loadLevel(loadLevel)
                .overdueCount(overdueCount)
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

        CandidateWorkloadImpact.CandidateWorkloadImpactBuilder b = CandidateWorkloadImpact.builder()
                .userId(userId).projectId(projectId);

        if (dailyCap == null || dailyCap.signum() <= 0) {
            LoadLevel lvl = dailyCap == null ? LoadLevel.UNDEFINED : LoadLevel.BLOCKED;
            return b.loadLevelBefore(lvl).loadLevelAfter(lvl)
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

        // Volume axis: backlog (workdays) before / after adding the candidate's effort.
        BigDecimal backlogHoursBefore = existing.stream()
                .map(ScheduledTask::remaining).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal candidateHours = estimatedHours != null ? estimatedHours : BigDecimal.ZERO;
        BigDecimal backlogDaysBefore = backlogHoursBefore.divide(dailyCap, 2, RoundingMode.HALF_UP);
        BigDecimal backlogDaysAfter = backlogHoursBefore.add(candidateHours)
                .divide(dailyCap, 2, RoundingMode.HALF_UP);

        return b
                .loadLevelBefore(loadLevelFor(backlogDaysBefore))
                .loadLevelAfter(loadLevelFor(backlogDaysAfter))
                .backlogDaysBefore(backlogDaysBefore)
                .backlogDaysAfter(backlogDaysAfter)
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

    private MemberWorkloadResponse toMemberResponse(User user, MemberLoad ml,
                                                    List<ProjectAllocationItem> projectItems,
                                                    List<TaskWorkloadItem> taskItems) {
        return MemberWorkloadResponse.builder()
                .userId(user.getId())
                .userFullName(user.getFullName())
                .email(user.getEmail())
                .loadLevel(ml.loadLevel())
                .worstBacklogDays(ml.worstBacklogDays())
                .atRiskCount(ml.atRiskCount())
                .overdueTaskCount(ml.overdueTaskCount())
                .predictedLateTaskCount(ml.predictedLateTaskCount())
                .unestimatedTaskCount(ml.unestimatedTaskCount())
                .activeTaskCount(ml.activeTaskCount())
                .projectAllocations(projectItems)
                .tasks(taskItems)
                .build();
    }
}
