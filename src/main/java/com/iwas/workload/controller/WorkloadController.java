package com.iwas.workload.controller;

import com.iwas.security.AuthenticatedUserResolver;
import com.iwas.workload.dto.BurnoutLogResponse;
import com.iwas.workload.dto.MemberWorkloadResponse;
import com.iwas.workload.dto.WorkloadSnapshotResponse;
import com.iwas.workload.service.WorkloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/workload")
@RequiredArgsConstructor
public class WorkloadController {

    private final WorkloadService workloadService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @GetMapping("/team")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'PROJECT_MANAGER')")
    public List<WorkloadSnapshotResponse> getTeamWorkload(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return workloadService.getTeamWorkload(date);
    }

    @GetMapping("/me")
    public List<WorkloadSnapshotResponse> getMyWorkload() {
        return workloadService.getUserWorkloadHistory(authenticatedUserResolver.currentUserId());
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'PROJECT_MANAGER')")
    public List<WorkloadSnapshotResponse> getUserWorkload(@PathVariable Long userId) {
        return workloadService.getUserWorkloadHistory(userId);
    }

    @PostMapping("/snapshots")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkloadSnapshotResponse takeSnapshot(@RequestParam Long userId) {
        return workloadService.takeSnapshot(userId);
    }

    @GetMapping("/projects/{projectId}/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public List<MemberWorkloadResponse> getProjectMembersWorkload(
            @PathVariable Long projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekEnd) {
        return workloadService.getProjectMembersWorkload(projectId, weekStart, weekEnd);
    }

    @GetMapping("/users/{userId}/realtime")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'PROJECT_MANAGER')")
    public MemberWorkloadResponse getUserWorkloadRealtime(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekEnd) {
        return workloadService.getUserWorkloadRealtime(userId, weekStart, weekEnd);
    }

    @GetMapping("/me/realtime")
    public MemberWorkloadResponse getMyWorkloadRealtime(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekEnd) {
        return workloadService.getUserWorkloadRealtime(
                authenticatedUserResolver.currentUserId(), weekStart, weekEnd);
    }

    @GetMapping("/burnout")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'PROJECT_MANAGER')")
    public List<BurnoutLogResponse> getBurnoutLogs() {
        return workloadService.getBurnoutLogs();
    }

    @GetMapping("/burnout/users/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'PROJECT_MANAGER')")
    public List<BurnoutLogResponse> getUserBurnoutHistory(@PathVariable Long userId) {
        return workloadService.getUserBurnoutHistory(userId);
    }
}
