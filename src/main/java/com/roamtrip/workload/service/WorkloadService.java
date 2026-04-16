package com.roamtrip.workload.service;

import com.roamtrip.project.repository.ProjectMemberRepository;
import com.roamtrip.task.repository.TaskRepository;
import com.roamtrip.user.entity.User;
import com.roamtrip.user.repository.UserRepository;
import com.roamtrip.workload.dto.BurnoutLogResponse;
import com.roamtrip.workload.dto.WorkloadSnapshotResponse;
import com.roamtrip.workload.entity.BurnoutLog;
import com.roamtrip.workload.entity.WorkloadSnapshot;
import com.roamtrip.workload.repository.BurnoutLogRepository;
import com.roamtrip.workload.repository.WorkloadSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkloadService {

    private final WorkloadSnapshotRepository workloadSnapshotRepository;
    private final BurnoutLogRepository burnoutLogRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;

    public List<WorkloadSnapshotResponse> getTeamWorkload(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<WorkloadSnapshot> snapshots = workloadSnapshotRepository.findBySnapshotDate(targetDate);
        return enrichSnapshots(snapshots);
    }

    public List<WorkloadSnapshotResponse> getUserWorkloadHistory(Long userId) {
        List<WorkloadSnapshot> snapshots = workloadSnapshotRepository.findByUserIdOrderByDateDesc(userId);
        return enrichSnapshots(snapshots);
    }

    public List<BurnoutLogResponse> getBurnoutLogs() {
        List<BurnoutLog> logs = burnoutLogRepository.findAll();
        return enrichBurnoutLogs(logs);
    }

    public List<BurnoutLogResponse> getUserBurnoutHistory(Long userId) {
        List<BurnoutLog> logs = burnoutLogRepository.findByUserIdOrderByEvaluatedAtDesc(userId);
        return enrichBurnoutLogs(logs);
    }

    @Transactional
    public WorkloadSnapshotResponse takeSnapshot(Long userId) {
        LocalDate today = LocalDate.now();

        int projectCount = projectMemberRepository.findActiveProjectsByUserId(userId).size();
        int activeTaskCount = taskRepository.findActiveTasksByAssigneeId(userId).size();

        BigDecimal estimatedHours = taskRepository.findActiveTasksByAssigneeId(userId).stream()
                .filter(t -> t.getEstimatedHours() != null)
                .map(t -> t.getEstimatedHours())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal actualHours = taskRepository.findActiveTasksByAssigneeId(userId).stream()
                .filter(t -> t.getActualHours() != null)
                .map(t -> t.getActualHours())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal capacityPercent = estimatedHours.compareTo(BigDecimal.ZERO) > 0
                ? actualHours.divide(estimatedHours, 2, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        WorkloadSnapshot snapshot = workloadSnapshotRepository
                .findByUserIdAndSnapshotDate(userId, today)
                .orElse(new WorkloadSnapshot());
        snapshot.setUserId(userId);
        snapshot.setSnapshotDate(today);
        snapshot.setTotalAllocatedHours(estimatedHours);
        snapshot.setTotalActualHours(actualHours);
        snapshot.setCapacityUsedPercent(capacityPercent);
        snapshot.setProjectCount(projectCount);
        snapshot.setActiveTaskCount(activeTaskCount);

        WorkloadSnapshot saved = workloadSnapshotRepository.save(snapshot);
        String name = userRepository.findById(userId).map(User::getFullName).orElse(null);
        return toSnapshotResponse(saved, name);
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
