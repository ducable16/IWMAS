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
import com.iwas.task.enums.TaskType;
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

    // ─── constants ────────────────────────────────────────────────────────────

    private static final double DEFAULT_DAILY_HOURS = 8.0;
    private static final int DEFAULT_HORIZON_WORKDAYS = 10;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    // ─── workday math ─────────────────────────────────────────────────────────

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

    // ─── task-level: remaining + daily rate ───────────────────────────────────

    private static BigDecimal typeDefaultHours(TaskType type) {
        if (type == null) return BigDecimal.valueOf(1.5);
        return switch (type) {
            case BUG -> BigDecimal.valueOf(2.0);
            case FEATURE -> BigDecimal.valueOf(4.0);
            default -> BigDecimal.valueOf(1.5);
        };
    }

    private BigDecimal remainingHours(Task task) {
        BigDecimal estimated = task.getEstimatedHours() != null
                ? task.getEstimatedHours()
                : typeDefaultHours(task.getType());
        BigDecimal actual = task.getActualHours() != null ? task.getActualHours() : BigDecimal.ZERO;
        return estimated.subtract(actual).max(BigDecimal.ZERO);
    }

    private record DailyRate(LocalDate effStart, LocalDate effEnd, BigDecimal hoursPerDay) {}

    /**
     * Linear-spread daily rate.
     * - Overdue tasks collapse onto today.
     * - Tasks with no dueDate are spread over DEFAULT_HORIZON_WORKDAYS.
     * Returns null when the task has zero remaining hours.
     */
    private DailyRate dailyRateOf(Task t, LocalDate today) {
        BigDecimal remaining = remainingHours(t);
        if (remaining.signum() <= 0) return null;

        LocalDate due = t.getDueDate();
        LocalDate start = t.getStartDate();
        LocalDate effStart, effEnd;

        if (due != null && due.isBefore(today)) {
            effStart = today;
            effEnd = today;
        } else if (due != null) {
            effStart = (start != null && start.isAfter(today)) ? start : today;
            effEnd = due;
            if (effEnd.isBefore(effStart)) effEnd = effStart;
        } else {
            effStart = (start != null && start.isAfter(today)) ? start : today;
            effEnd = addWorkdays(effStart, DEFAULT_HORIZON_WORKDAYS - 1);
        }

        long workdays = countWorkdays(effStart, effEnd);
        if (workdays <= 0) workdays = 1;
        BigDecimal rate = remaining.divide(BigDecimal.valueOf(workdays), 4, RoundingMode.HALF_UP);
        return new DailyRate(effStart, effEnd, rate);
    }

    private BigDecimal loadInWindow(List<Task> tasks, LocalDate from, LocalDate to, LocalDate today) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Task t : tasks) {
            DailyRate dr = dailyRateOf(t, today);
            if (dr == null) continue;
            long ov = overlapWorkdays(from, to, dr.effStart(), dr.effEnd());
            if (ov > 0) {
                sum = sum.add(dr.hoursPerDay().multiply(BigDecimal.valueOf(ov)));
            }
        }
        return sum.setScale(1, RoundingMode.HALF_UP);
    }

    private boolean taskTouchesWindow(Task t, LocalDate from, LocalDate to, LocalDate today) {
        DailyRate dr = dailyRateOf(t, today);
        if (dr == null) return false;
        return overlapWorkdays(from, to, dr.effStart(), dr.effEnd()) > 0;
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

        List<Task> touching = activeTasks.stream()
                .filter(t -> taskTouchesWindow(t, from, to, today))
                .toList();

        return new TotalUtilization(capacity, load, utilization, level,
                activeTasks.size(), overdueCount, touching);
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
        snapshot.setWeeklyRemainingHours(total.loadHours());
        snapshot.setUtilizationPercent(total.utilizationPercent());
        snapshot.setWorkloadLevel(total.workloadLevel());
        snapshot.setOverdueTaskCount(total.overdueTaskCount());

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
                .weeklyRemainingHours(total.loadHours())
                .utilizationPercent(total.utilizationPercent())
                .workloadLevel(total.workloadLevel())
                .activeTaskCount(total.activeTaskCount())
                .overdueTaskCount(total.overdueTaskCount())
                .projectAllocations(projectItems)
                .tasks(taskItems)
                .build();
    }

    private MemberWorkloadResponse.TaskWorkloadItem toTaskItem(Task task, LocalDate today) {
        boolean overdue = task.getDueDate() != null && task.getDueDate().isBefore(today);
        return MemberWorkloadResponse.TaskWorkloadItem.builder()
                .taskId(task.getId())
                .title(task.getTitle())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .remainingHours(remainingHours(task))
                .overdue(overdue)
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
                .weeklyRemainingHours(ws.getWeeklyRemainingHours())
                .utilizationPercent(ws.getUtilizationPercent())
                .workloadLevel(ws.getWorkloadLevel())
                .overdueTaskCount(ws.getOverdueTaskCount())
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
