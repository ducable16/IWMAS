package com.iwas.workload.controller;

import com.iwas.security.AuthenticatedUserResolver;
import com.iwas.workload.dto.MemberWorkloadResponse;
import com.iwas.workload.dto.ProjectMemberWorkloadResponse;
import com.iwas.workload.dto.ProjectScheduleResponse;
import com.iwas.workload.dto.SchedulePreviewRequest;
import com.iwas.workload.service.WorkloadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workload")
@RequiredArgsConstructor
public class WorkloadController {

    private final WorkloadService workloadService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @GetMapping("/projects/{projectId}/members")
    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    public List<ProjectMemberWorkloadResponse> getProjectMembersWorkload(@PathVariable Long projectId) {
        return workloadService.getProjectMembersWorkload(projectId, authenticatedUserResolver.currentUserId());
    }

    @GetMapping("/users/{userId}/realtime")
    @PreAuthorize("hasAnyRole('HR', 'PROJECT_MANAGER')")
    public MemberWorkloadResponse getUserWorkloadRealtime(@PathVariable Long userId) {
        return workloadService.getUserWorkloadRealtime(userId);
    }

    @GetMapping("/me/realtime")
    public MemberWorkloadResponse getMyWorkloadRealtime() {
        return workloadService.getUserWorkloadRealtime(authenticatedUserResolver.currentUserId());
    }

    // ─── what-if scheduling for the current member ────────────────────────────

    @GetMapping("/me/schedule")
    public ProjectScheduleResponse getMySchedule(@RequestParam Long projectId) {
        return workloadService.getMySchedule(authenticatedUserResolver.currentUserId(), projectId);
    }

    @GetMapping("/me/schedule/suggest")
    public ProjectScheduleResponse suggestMySchedule(@RequestParam Long projectId) {
        return workloadService.suggestSchedule(authenticatedUserResolver.currentUserId(), projectId);
    }

    @PostMapping("/me/schedule/preview")
    public ProjectScheduleResponse previewMySchedule(@Valid @RequestBody SchedulePreviewRequest request) {
        return workloadService.previewSchedule(authenticatedUserResolver.currentUserId(),
                request.getProjectId(), request.getOrderedTaskIds());
    }

    @PutMapping("/me/schedule")
    public ProjectScheduleResponse saveMySchedule(@Valid @RequestBody SchedulePreviewRequest request) {
        return workloadService.saveSchedule(authenticatedUserResolver.currentUserId(),
                request.getProjectId(), request.getOrderedTaskIds());
    }
}
