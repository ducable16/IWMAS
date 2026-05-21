package com.iwas.workload.service;

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
import com.iwas.task.repository.TaskRepository;
import com.iwas.user.entity.User;
import com.iwas.user.repository.UserRepository;
import com.iwas.workload.dto.BurnoutLogResponse;
import com.iwas.workload.dto.MemberWorkloadResponse;
import com.iwas.workload.dto.WorkloadSnapshotResponse;
import com.iwas.workload.entity.BurnoutLog;
import com.iwas.workload.entity.WorkloadSnapshot;
import com.iwas.workload.enums.WorkloadLevel;
import com.iwas.workload.repository.BurnoutLogRepository;
import com.iwas.workload.repository.WorkloadSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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


    private static final double DEFAULT_DAILY_HOURS = 8.0;
    private static final int DEFAULT_HORIZON_WORKDAYS = 10;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private static boolean isWorkday(LocalDate d) {
        DayOfWeek dow = d.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }

    private static long countWorkdays(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) return 0;
        return from.datesUntil(to.plusDays(1))
                .filter(WorkloadService::isWorkday)
                .count();
    }

    private static LocalDate addWorkdays(LocalDate start, int workdaysToAdd) {
        if (workdaysToAdd <= 0) return start;
        LocalDate d = start;
        int added = 0;
        while (added < workdaysToAdd) {
            d = d.plusDays(1);
            if (isWorkday(d)) added++;
        }
        return d;
    }

    private static long overlapWorkdays(LocalDate winFrom, LocalDate winTo,
                                        LocalDate effStart, LocalDate effEnd) {
        LocalDate ovStart = winFrom.isAfter(effStart) ? winFrom : effStart;
        LocalDate ovEnd = winTo.isBefore(effEnd) ? winTo : effEnd;
        if (ovEnd.isBefore(ovStart)) return 0;
        return countWorkdays(ovStart, ovEnd);
    }

    // ─── task-level: virtual burn ─────────────────────────────────────────────

    private record TaskBurn(LocalDate effStart, LocalDate effEnd,
                            BigDecimal dailyBurn, BigDecimal remaining) {}

    /**
     * Virtual-burn schedule for a task.
     *
     * After TaskService.resolveAndApplyDates() runs at the API boundary,
     * startDate and dueDate are guaranteed non-null on new/updated tasks. The
     * null fallbacks below are defensive for legacy rows that pre-date the
     * validation.
     *
     * Formula:
     *  - Overdue (due < today): collapse to today, full estimated as load today.
     *  - Normal: dailyBurn = estimated / span(start, due) — fixed at scheduling.
     *            remaining = max(0, estimated − dailyBurn × elapsed workdays).
     *
     * Returns null in three cases (caller must skip the task):
     *  - estimatedHours is null or non-positive (task is unestimated — counted
     *    separately as `unestimatedTaskCount` so PMs see the risk explicitly
     *    rather than via a silent type-based default).
     *  - virtual burn has consumed the full estimate (task is "burned out").
     *  - both startDate and dueDate are null (legacy data, defensive).
     *
     * actualHours is intentionally NOT consulted here. Time logs are a
     * reporting record (snapshot.totalActualHours, capacityUsedPercent); the
     * workload model derives load from schedule alone so it stays robust
     * against teams with inconsistent log-time discipline.
     */
    private TaskBurn computeBurn(Task task, LocalDate today) {
        BigDecimal estimated = task.getEstimatedHours();
        if (estimated == null || estimated.signum() <= 0) return null;

        LocalDate start = task.getStartDate();
        LocalDate due = task.getDueDate();
        if (start == null && due == null) return null;
        if (start == null) start = today;
        if (due == null) due = addWorkdays(start, DEFAULT_HORIZON_WORKDAYS - 1);

        if (due.isBefore(today)) {
            return new TaskBurn(today, today, estimated, estimated);
        }

        LocalDate effStart = start.isAfter(today) ? start : today;
        LocalDate effEnd = due.isBefore(effStart) ? effStart : due;

        long span = countWorkdays(start, due);
        if (span <= 0) span = 1;
        BigDecimal dailyBurn = estimated.divide(BigDecimal.valueOf(span), 4, RoundingMode.HALF_UP);

        long elapsed = 0;
        if (today.isAfter(start)) {
            elapsed = Math.min(span, countWorkdays(start, today.minusDays(1)));
        }
        BigDecimal remaining = estimated
                .subtract(dailyBurn.multiply(BigDecimal.valueOf(elapsed)))
                .max(BigDecimal.ZERO);
        if (remaining.signum() <= 0) return null;

        return new TaskBurn(effStart, effEnd, dailyBurn, remaining);
    }

    private BigDecimal loadInWindow(List<Task> tasks, LocalDate from, LocalDate to, LocalDate today) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Task task : tasks) {
            TaskBurn tb = computeBurn(task, today);
            if (tb == null) continue;
            long ov = overlapWorkdays(from, to, tb.effStart(), tb.effEnd());
            if (ov > 0) {
                sum = sum.add(tb.dailyBurn().multiply(BigDecimal.valueOf(ov)));
            }
        }
        return sum.setScale(1, RoundingMode.HALF_UP);
    }

    private boolean taskTouchesWindow(Task task, LocalDate from, LocalDate to, LocalDate today) {
        TaskBurn tb = computeBurn(task, today);
        if (tb != null) {
            return overlapWorkdays(from, to, tb.effStart(), tb.effEnd()) > 0;
        }
        // Unestimated tasks bypass virtual burn but should still surface in the
        // touching list so PMs can act on them — fall back to raw date overlap.
        if (!isUnestimated(task)) return false;
        LocalDate start = task.getStartDate();
        LocalDate due = task.getDueDate();
        if (start == null && due == null) return false;
        if (start == null) start = today;
        if (due == null) due = addWorkdays(start, DEFAULT_HORIZON_WORKDAYS - 1);
        if (due.isBefore(today)) return overlapWorkdays(from, to, today, today) > 0;
        LocalDate effStart = start.isAfter(today) ? start : today;
        return overlapWorkdays(from, to, effStart, due) > 0;
    }

    private static boolean isUnestimated(Task task) {
        return task.getEstimatedHours() == null || task.getEstimatedHours().signum() <= 0;
    }

    private static WorkloadLevel toWorkloadLevel(BigDecimal utilizationPercent) {
        if (utilizationPercent.compareTo(HUNDRED) > 0) return WorkloadLevel.OVERLOADED;
        if (utilizationPercent.compareTo(BigDecimal.valueOf(70)) >= 0) return WorkloadLevel.HEALTHY_BUSY;
        return WorkloadLevel.AVAILABLE;
    }

    // ─── total utilization (per user, across all projects) ────────────────────

    private record TotalUtilization(
            BigDecimal capacityHours,
            BigDecimal loadHours,
            BigDecimal utilizationPercent,
            WorkloadLevel workloadLevel,
            int activeTaskCount,
            int overdueTaskCount,
            int unestimatedTaskCount,
            List<Task> tasksTouchingWindow
    ) {}

    private TotalUtilization computeTotal(List<Task> activeTasks, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        long workdays = countWorkdays(from, to);
        BigDecimal capacity = BigDecimal.valueOf(workdays * DEFAULT_DAILY_HOURS)
                .setScale(1, RoundingMode.HALF_UP);
        BigDecimal load = loadInWindow(activeTasks, from, to, today);

        BigDecimal utilization;
        WorkloadLevel level;
        if (capacity.signum() <= 0) {
            utilization = BigDecimal.ZERO;
            level = WorkloadLevel.UNDEFINED;
        } else {
            utilization = load.divide(capacity, 4, RoundingMode.HALF_UP)
                    .multiply(HUNDRED)
                    .setScale(2, RoundingMode.HALF_UP);
            level = toWorkloadLevel(utilization);
        }

        int overdueCount = (int) activeTasks.stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(today))
                .count();

        int unestimatedCount = (int) activeTasks.stream()
                .filter(WorkloadService::isUnestimated)
                .count();

        List<Task> touching = activeTasks.stream()
                .filter(t -> taskTouchesWindow(t, from, to, today))
                .toList();

        return new TotalUtilization(capacity, load, utilization, level,
                activeTasks.size(), overdueCount, unestimatedCount, touching);
    }

    // ─── per-project utilization ──────────────────────────────────────────────

    /**
     * Per-project utilization in percent. Returns null when the user has no
     * positive allocation on this project (manager-without-row, alloc=0, or
     * zero-workday window). Callers map null → BLOCKED / UNDEFINED.
     * Public so the recommender can read availability for a candidate task.
     */
    public BigDecimal utilizationPerProject(Long userId, Long projectId,
                                            LocalDate from, LocalDate to) {
        long workdays = countWorkdays(from, to);
        if (workdays == 0) return null;

        Integer alloc = projectMemberRepository
                .findActiveMemberByProjectIdAndUserId(projectId, userId)
                .map(ProjectMember::getAllocatedEffortPercent)
                .orElse(null);
        if (alloc == null || alloc == 0) return null;

        double dailyCap = DEFAULT_DAILY_HOURS * alloc / 100.0;
        BigDecimal capacity = BigDecimal.valueOf(workdays * dailyCap)
                .setScale(1, RoundingMode.HALF_UP);
        if (capacity.signum() <= 0) return null;

        List<Task> tasks = taskRepository.findActiveTasksByAssigneeId(userId).stream()
                .filter(t -> projectId.equals(t.getProjectId()))
                .toList();
        BigDecimal load = loadInWindow(tasks, from, to, LocalDate.now());

        return load.divide(capacity, 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
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
    public List<MemberWorkloadResponse> getProjectMembersWorkload(Long projectId,
                                                                   LocalDate weekStart,
                                                                   LocalDate weekEnd) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        LocalDate[] win = resolveWindow(weekStart, weekEnd);

        LinkedHashSet<Long> userIds = new LinkedHashSet<>();
        if (project.getManagerId() != null) userIds.add(project.getManagerId());
        List<ProjectMember> activeMembers = projectMemberRepository.findActiveMembersByProjectId(projectId);
        activeMembers.forEach(m -> userIds.add(m.getUserId()));

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, ProjectMember> memberByUser = activeMembers.stream()
                .collect(Collectors.toMap(ProjectMember::getUserId, m -> m, (a, b) -> a));

        return userIds.stream()
                .filter(uid -> userMap.containsKey(uid)
                        && !Boolean.TRUE.equals(userMap.get(uid).getIsDeleted()))
                .map(uid -> {
                    User user = userMap.get(uid);
                    List<Task> activeTasks = taskRepository.findActiveTasksByAssigneeId(uid);
                    TotalUtilization total = computeTotal(activeTasks, win[0], win[1]);
                    ProjectMember pmRow = memberByUser.get(uid);
                    MemberWorkloadResponse.ProjectAllocationItem projectItem =
                            buildProjectItem(user, project, pmRow, activeTasks, win[0], win[1]);
                    return toMemberResponse(user, total, win[0], win[1],
                            List.of(projectItem), false);
                })
                .toList();
    }

    /** Real-time workload for a single user — total + per-project breakdown + task list. */
    public MemberWorkloadResponse getUserWorkloadRealtime(Long userId,
                                                          LocalDate weekStart,
                                                          LocalDate weekEnd) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        LocalDate[] win = resolveWindow(weekStart, weekEnd);
        List<Task> activeTasks = taskRepository.findActiveTasksByAssigneeId(userId);
        TotalUtilization total = computeTotal(activeTasks, win[0], win[1]);

        // Union of: projects where user is an active member + projects where user is manager
        List<ProjectMember> memberships = projectMemberRepository.findActiveProjectsByUserId(userId);
        Set<Long> projectIds = new LinkedHashSet<>();
        memberships.forEach(pm -> projectIds.add(pm.getProjectId()));
        projectRepository.findByManagerId(userId).forEach(p -> projectIds.add(p.getId()));

        Map<Long, Project> projectMap = projectRepository.findAllById(projectIds).stream()
                .collect(Collectors.toMap(Project::getId, p -> p));
        Map<Long, ProjectMember> memberByProject = memberships.stream()
                .collect(Collectors.toMap(ProjectMember::getProjectId, m -> m, (a, b) -> a));

        List<MemberWorkloadResponse.ProjectAllocationItem> projectItems = projectIds.stream()
                .map(pid -> {
                    Project p = projectMap.get(pid);
                    if (p == null) return null;
                    return buildProjectItem(user, p, memberByProject.get(pid),
                            activeTasks, win[0], win[1]);
                })
                .filter(Objects::nonNull)
                .toList();

        return toMemberResponse(user, total, win[0], win[1], projectItems, true);
    }

    @Transactional
    public WorkloadSnapshotResponse takeSnapshot(Long userId) {
        LocalDate today = LocalDate.now();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = today.with(DayOfWeek.SUNDAY);

        int projectCount = projectMemberRepository.findActiveProjectsByUserId(userId).size();
        List<Task> activeTasks = taskRepository.findActiveTasksByAssigneeId(userId);

        BigDecimal estimatedHours = activeTasks.stream()
                .filter(t -> t.getEstimatedHours() != null)
                .map(Task::getEstimatedHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal actualHours = activeTasks.stream()
                .filter(t -> t.getActualHours() != null)
                .map(Task::getActualHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal completionRate = estimatedHours.compareTo(BigDecimal.ZERO) > 0
                ? actualHours.divide(estimatedHours, 2, RoundingMode.HALF_UP).multiply(HUNDRED)
                : BigDecimal.ZERO;

        TotalUtilization total = computeTotal(activeTasks, weekStart, weekEnd);

        WorkloadSnapshot snapshot = workloadSnapshotRepository
                .findByUserIdAndSnapshotDate(userId, today)
                .orElse(new WorkloadSnapshot());
        snapshot.setUserId(userId);
        snapshot.setSnapshotDate(today);
        snapshot.setTotalAllocatedHours(estimatedHours);
        snapshot.setTotalActualHours(actualHours);
        snapshot.setCapacityUsedPercent(completionRate);
        snapshot.setProjectCount(projectCount);
        snapshot.setActiveTaskCount(total.activeTaskCount());
        snapshot.setWeeklyCapacityHours(total.capacityHours());
        snapshot.setWeeklyLoadHours(total.loadHours());
        snapshot.setUtilizationPercent(total.utilizationPercent());
        snapshot.setWorkloadLevel(total.workloadLevel());
        snapshot.setOverdueTaskCount(total.overdueTaskCount());
        snapshot.setUnestimatedTaskCount(total.unestimatedTaskCount());

        WorkloadSnapshot saved = workloadSnapshotRepository.save(snapshot);

        if (total.workloadLevel() == WorkloadLevel.OVERLOADED) {
            notificationService.send(
                    userId, NotificationType.OVERLOAD_WARNING,
                    NotificationMessages.overloadWarning(total.utilizationPercent().toPlainString()),
                    "WORKLOAD", saved.getId());
        }

        return toSnapshotResponse(saved, user.getFullName());
    }

    // ─── private mapping ──────────────────────────────────────────────────────

    private LocalDate[] resolveWindow(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        return new LocalDate[]{
                from != null ? from : today.with(DayOfWeek.MONDAY),
                to != null ? to : today.with(DayOfWeek.SUNDAY)
        };
    }

    private MemberWorkloadResponse.ProjectAllocationItem buildProjectItem(
            User user, Project project, ProjectMember pmRow,
            List<Task> userActiveTasks, LocalDate from, LocalDate to) {

        long workdays = countWorkdays(from, to);
        Integer alloc = pmRow != null ? pmRow.getAllocatedEffortPercent() : null;
        boolean isManagerOnly = pmRow == null && user.getId().equals(project.getManagerId());

        BigDecimal load = loadInWindow(
                userActiveTasks.stream()
                        .filter(t -> project.getId().equals(t.getProjectId()))
                        .toList(),
                from, to, LocalDate.now());

        // Manager without alloc row → no per-project capacity
        if (isManagerOnly || alloc == null) {
            return MemberWorkloadResponse.ProjectAllocationItem.builder()
                    .projectId(project.getId())
                    .projectName(project.getName())
                    .allocatedEffortPercent(alloc)
                    .dailyCapacityHours(null)
                    .loadInWindowHours(load)
                    .utilizationPercent(null)
                    .workloadLevel(WorkloadLevel.UNDEFINED)
                    .build();
        }

        // Observer / no contracted hours
        if (alloc == 0) {
            return MemberWorkloadResponse.ProjectAllocationItem.builder()
                    .projectId(project.getId())
                    .projectName(project.getName())
                    .allocatedEffortPercent(0)
                    .dailyCapacityHours(BigDecimal.ZERO)
                    .loadInWindowHours(load)
                    .utilizationPercent(null)
                    .workloadLevel(WorkloadLevel.BLOCKED)
                    .build();
        }

        double dailyCap = DEFAULT_DAILY_HOURS * alloc / 100.0;
        BigDecimal capacity = BigDecimal.valueOf(workdays * dailyCap)
                .setScale(1, RoundingMode.HALF_UP);

        BigDecimal utilization;
        WorkloadLevel level;
        if (capacity.signum() <= 0) {
            utilization = null;
            level = WorkloadLevel.UNDEFINED;
        } else {
            utilization = load.divide(capacity, 4, RoundingMode.HALF_UP)
                    .multiply(HUNDRED)
                    .setScale(2, RoundingMode.HALF_UP);
            level = toWorkloadLevel(utilization);
        }

        return MemberWorkloadResponse.ProjectAllocationItem.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .allocatedEffortPercent(alloc)
                .dailyCapacityHours(BigDecimal.valueOf(dailyCap).setScale(2, RoundingMode.HALF_UP))
                .loadInWindowHours(load)
                .utilizationPercent(utilization)
                .workloadLevel(level)
                .build();
    }

    private MemberWorkloadResponse toMemberResponse(User user, TotalUtilization total,
                                                     LocalDate weekStart, LocalDate weekEnd,
                                                     List<MemberWorkloadResponse.ProjectAllocationItem> projectItems,
                                                     boolean includeTasks) {
        LocalDate today = LocalDate.now();
        List<MemberWorkloadResponse.TaskWorkloadItem> taskItems = null;
        if (includeTasks) {
            taskItems = total.tasksTouchingWindow().stream()
                    .map(t -> toTaskItem(t, today))
                    .toList();
        }
        return MemberWorkloadResponse.builder()
                .userId(user.getId())
                .userFullName(user.getFullName())
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .weeklyCapacityHours(total.capacityHours())
                .weeklyLoadHours(total.loadHours())
                .utilizationPercent(total.utilizationPercent())
                .workloadLevel(total.workloadLevel())
                .activeTaskCount(total.activeTaskCount())
                .overdueTaskCount(total.overdueTaskCount())
                .unestimatedTaskCount(total.unestimatedTaskCount())
                .projectAllocations(projectItems)
                .tasks(taskItems)
                .build();
    }

    private MemberWorkloadResponse.TaskWorkloadItem toTaskItem(Task task, LocalDate today) {
        boolean overdue = task.getDueDate() != null && task.getDueDate().isBefore(today);
        TaskBurn tb = computeBurn(task, today);
        BigDecimal remaining = tb != null
                ? tb.remaining().setScale(2, RoundingMode.HALF_UP)
                : null;
        return MemberWorkloadResponse.TaskWorkloadItem.builder()
                .taskId(task.getId())
                .title(task.getTitle())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .remainingHours(remaining)
                .overdue(overdue)
                .unestimated(isUnestimated(task))
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
                .totalAllocatedHours(ws.getTotalAllocatedHours())
                .totalActualHours(ws.getTotalActualHours())
                .capacityUsedPercent(ws.getCapacityUsedPercent())
                .projectCount(ws.getProjectCount())
                .activeTaskCount(ws.getActiveTaskCount())
                .weeklyCapacityHours(ws.getWeeklyCapacityHours())
                .weeklyLoadHours(ws.getWeeklyLoadHours())
                .utilizationPercent(ws.getUtilizationPercent())
                .workloadLevel(ws.getWorkloadLevel())
                .overdueTaskCount(ws.getOverdueTaskCount())
                .unestimatedTaskCount(ws.getUnestimatedTaskCount())
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
