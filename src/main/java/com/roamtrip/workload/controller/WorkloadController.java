package com.roamtrip.workload.controller;

import com.roamtrip.security.AuthenticatedUserResolver;
import com.roamtrip.workload.dto.BurnoutLogResponse;
import com.roamtrip.workload.dto.WorkloadSnapshotResponse;
import com.roamtrip.workload.service.WorkloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<WorkloadSnapshotResponse>> getTeamWorkload(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(workloadService.getTeamWorkload(date));
    }

    @GetMapping("/me")
    public ResponseEntity<List<WorkloadSnapshotResponse>> getMyWorkload() {
        return ResponseEntity.ok(workloadService.getUserWorkloadHistory(authenticatedUserResolver.currentUserId()));
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'PROJECT_MANAGER')")
    public ResponseEntity<List<WorkloadSnapshotResponse>> getUserWorkload(@PathVariable Long userId) {
        return ResponseEntity.ok(workloadService.getUserWorkloadHistory(userId));
    }

    @PostMapping("/snapshot")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<WorkloadSnapshotResponse> takeSnapshot(@RequestParam Long userId) {
        return ResponseEntity.ok(workloadService.takeSnapshot(userId));
    }

    @GetMapping("/burnout")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'PROJECT_MANAGER')")
    public ResponseEntity<List<BurnoutLogResponse>> getBurnoutLogs() {
        return ResponseEntity.ok(workloadService.getBurnoutLogs());
    }

    @GetMapping("/burnout/users/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'PROJECT_MANAGER')")
    public ResponseEntity<List<BurnoutLogResponse>> getUserBurnoutHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(workloadService.getUserBurnoutHistory(userId));
    }
}
