package com.iwas.workload.service;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.notification.enums.NotificationType;
import com.iwas.notification.service.NotificationService;
import com.iwas.project.entity.Project;
import com.iwas.project.repository.ProjectMemberRepository;
import com.iwas.project.repository.ProjectRepository;
import com.iwas.task.entity.Task;
import com.iwas.task.enums.TaskType;
import com.iwas.task.repository.TaskRepository;
import com.iwas.user.entity.User;
import com.iwas.user.enums.UserRole;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    // ─── capacity constants ───────────────────────────────────────────────────

    private static final double DEFAULT_HOURS_PER_DAY = 6.0;

    private static double hoursPerDayForRole(UserRole role) {
        if (role == null) return DEFAULT_HOURS_PER_DAY;
        if (role == UserRole.PROJECT_MANAGER)
            return 5.5;
        if (role == UserRole.TEAM_MEMBER)
            return 6.5;
        return DEFAULT_HOURS_PER_DAY;
    }

    private static BigDecimal typeDefaultHours(TaskType type) {
        return switch (type) {
            case BUG -> BigDecimal.valueOf(2.0);
            case FEATURE -> BigDecimal.valueOf(4.0);
            default -> BigDecimal.valueOf(1.5);
        };
    }

    // ─── private record ───────────────────────────────────────────────────────

    private record UtilizationResult(
            BigDecimal weeklyCapacityHours,
            BigDecimal weeklyRemainingHours,
            BigDecimal utilizationPercent,
            WorkloadLevel workloadLevel,
            int activeTaskCount,
            int overdueTaskCount,
            List<Task> weekTasks,
            List<Task> overdueTasks
    ) {}

    // ─── core helpers ─────────────────────────────────────────────────────────

    private BigDecimal effectiveCapacityHours(User user, LocalDate from, LocalDate to) {
        double hoursPerDay = hoursPerDayForRole(user.getRole());
        long workdays = from.datesUntil(to.plusDays(1))
                .filter(d -> d.getDayOfWeek() != DayOfWeek.SATURDAY
                          && d.getDayOfWeek() != DayOfWeek.SUNDAY)
                .count();
        return BigDecimal.valueOf(workdays * hoursPerDay).setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal remainingHours(Task task) {
        BigDecimal estimated = task.getEstimatedHours() != null
                ? task.getEstimatedHours()
                : typeDefaultHours(task.getType());
        BigDecimal actual = task.getActualHours() != null ? task.getActualHours() : BigDecimal.ZERO;
        return estimated.subtract(actual).max(BigDecimal.ZERO);
    }

    private WorkloadLevel toWorkloadLevel(BigDecimal utilizationPercent) {
        if (utilizationPercent.compareTo(BigDecimal.valueOf(100)) > 0) return WorkloadLevel.OVERLOADED;
        if (utilizationPercent.compareTo(BigDecimal.valueOf(70)) >= 0) return WorkloadLevel.HEALTHY_BUSY;
        return WorkloadLevel.AVAILABLE;
    }

    private UtilizationResult computeUtilization(User user, LocalDate weekStart, LocalDate weekEnd) {
        LocalDate today = LocalDate.now();

        BigDecimal capacityHours = effectiveCapacityHours(user, weekStart, weekEnd);

        List<Task> weekTasks = taskRepository
                .findActiveTasksDueBetweenByAssignee(user.getId(), weekStart, weekEnd);
        BigDecimal remainingHrs = weekTasks.stream()
                .map(this::remainingHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(1, RoundingMode.HALF_UP);

        BigDecimal utilization = capacityHours.compareTo(BigDecimal.ZERO) > 0
                ? remainingHrs.divide(capacityHours, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<Task> overdueTasks = taskRepository.findOverdueTasksByAssignee(user.getId(), today);
        int activeCount = taskRepository.findActiveTasksByAssigneeId(user.getId()).size();

        return new UtilizationResult(capacityHours, remainingHrs, utilization,
                toWorkloadLevel(utilization), activeCount, overdueTasks.size(), weekTasks, overdueTasks);
    }

    private LocalDate[] resolveWeek(LocalDate weekStart, LocalDate weekEnd) {
        LocalDate today = LocalDate.now();
        return new LocalDate[]{
                weekStart != null ? weekStart : today.with(DayOfWeek.MONDAY),
                weekEnd   != null ? weekEnd   : today.with(DayOfWeek.SUNDAY)
        };
    }

    // ─── public API ───────────────────────────────────────────────────────────

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

    /** Real-time workload for all active members of a project (no task breakdown). */
    public List<MemberWorkloadResponse> getProjectMembersWorkload(Long projectId,
                                                                   LocalDate weekStart,
                                                                   LocalDate weekEnd) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        LocalDate[] week = resolveWeek(weekStart, weekEnd);

        // manager first, then members — preserving insertion order
        LinkedHashSet<Long> userIds = new LinkedHashSet<>();
        userIds.add(project.getManagerId());
        projectMemberRepository.findActiveMembersByProjectId(projectId)
                .forEach(m -> userIds.add(m.getUserId()));

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return userIds.stream()
                .filter(uid -> userMap.containsKey(uid)
                        && !Boolean.TRUE.equals(userMap.get(uid).getIsDeleted()))
                .map(uid -> {
                    User user = userMap.get(uid);
                    UtilizationResult util = computeUtilization(user, week[0], week[1]);
                    return toMemberResponse(user, util, week[0], week[1], false);
                })
                .toList();
    }

    /** Real-time workload for a single user, including full task breakdown. */
    public MemberWorkloadResponse getUserWorkloadRealtime(Long userId,
                                                          LocalDate weekStart,
                                                          LocalDate weekEnd) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        LocalDate[] week = resolveWeek(weekStart, weekEnd);
        UtilizationResult util = computeUtilization(user, week[0], week[1]);
        return toMemberResponse(user, util, week[0], week[1], true);
    }

    @Transactional
    public WorkloadSnapshotResponse takeSnapshot(Long userId) {
        LocalDate today = LocalDate.now();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd   = today.with(DayOfWeek.SUNDAY);

        // existing: project count + completion rate (actual / estimated)
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
                ? actualHours.divide(estimatedHours, 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // new: utilization-based workload
        UtilizationResult util = computeUtilization(user, weekStart, weekEnd);

        WorkloadSnapshot snapshot = workloadSnapshotRepository
                .findByUserIdAndSnapshotDate(userId, today)
                .orElse(new WorkloadSnapshot());
        snapshot.setUserId(userId);
        snapshot.setSnapshotDate(today);
        snapshot.setTotalAllocatedHours(estimatedHours);
        snapshot.setTotalActualHours(actualHours);
        snapshot.setCapacityUsedPercent(completionRate);
        snapshot.setProjectCount(projectCount);
        snapshot.setActiveTaskCount(util.activeTaskCount());
        snapshot.setWeeklyCapacityHours(util.weeklyCapacityHours());
        snapshot.setWeeklyRemainingHours(util.weeklyRemainingHours());
        snapshot.setUtilizationPercent(util.utilizationPercent());
        snapshot.setWorkloadLevel(util.workloadLevel());
        snapshot.setOverdueTaskCount(util.overdueTaskCount());

        WorkloadSnapshot saved = workloadSnapshotRepository.save(snapshot);

        if (util.workloadLevel() == WorkloadLevel.OVERLOADED) {
            notificationService.send(
                    userId, NotificationType.OVERLOAD_WARNING,
                    "Cảnh báo quá tải công việc",
                    "Workload tuần này đạt " + util.utilizationPercent().toPlainString()
                            + "% năng lực. Hãy kiểm tra và điều chỉnh khối lượng công việc.",
                    "WORKLOAD", saved.getId());
        }

        return toSnapshotResponse(saved, user.getFullName());
    }

    // ─── private mapping ─────────────────────────────────────────────────────

    private MemberWorkloadResponse toMemberResponse(User user, UtilizationResult util,
                                                     LocalDate weekStart, LocalDate weekEnd,
                                                     boolean includeTasks) {
        List<MemberWorkloadResponse.TaskWorkloadItem> taskItems = null;
        if (includeTasks) {
            taskItems = Stream.concat(
                    util.overdueTasks().stream().map(t -> toTaskItem(t, true)),
                    util.weekTasks().stream().map(t -> toTaskItem(t, false))
            ).toList();
        }
        return MemberWorkloadResponse.builder()
                .userId(user.getId())
                .userFullName(user.getFullName())
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .weeklyCapacityHours(util.weeklyCapacityHours())
                .weeklyRemainingHours(util.weeklyRemainingHours())
                .utilizationPercent(util.utilizationPercent())
                .workloadLevel(util.workloadLevel())
                .activeTaskCount(util.activeTaskCount())
                .overdueTaskCount(util.overdueTaskCount())
                .tasks(taskItems)
                .build();
    }

    private MemberWorkloadResponse.TaskWorkloadItem toTaskItem(Task task, boolean overdue) {
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
