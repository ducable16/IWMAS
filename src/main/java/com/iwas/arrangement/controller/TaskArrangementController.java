package com.iwas.arrangement.controller;

import com.iwas.arrangement.dto.ArrangeResponse;
import com.iwas.arrangement.dto.NextTaskResponse;
import com.iwas.arrangement.service.TaskArrangementService;
import com.iwas.security.AuthenticatedUserResolver;
import com.iwas.task.enums.TaskPriority;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumMap;
import java.util.Map;

@RestController
@RequestMapping("/api/arrangement")
@RequiredArgsConstructor
public class TaskArrangementController {

    private final TaskArrangementService arrangementService;
    private final AuthenticatedUserResolver authenticatedUserResolver;


    @GetMapping("/lanes/{projectId}/{assigneeId}")
    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    public ArrangeResponse arrange(@PathVariable Long projectId, @PathVariable Long assigneeId,
                                   @RequestParam(required = false) Double k,
                                   @RequestParam(required = false) Double wCritical,
                                   @RequestParam(required = false) Double wHigh,
                                   @RequestParam(required = false) Double wMedium,
                                   @RequestParam(required = false) Double wLow) {
        return arrangementService.arrangeLane(projectId, assigneeId, k,
                weightOverrides(wCritical, wHigh, wMedium, wLow));
    }

    @GetMapping("/lanes/{projectId}/{assigneeId}/next")
    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    public NextTaskResponse next(@PathVariable Long projectId, @PathVariable Long assigneeId,
                                 @RequestParam(required = false) Double k) {
        return arrangementService.nextTask(projectId, assigneeId, k, null);
    }

    @GetMapping("/me/lanes/{projectId}")
    public ArrangeResponse arrangeMine(@PathVariable Long projectId,
                                       @RequestParam(required = false) Double k) {
        return arrangementService.arrangeLane(projectId,
                authenticatedUserResolver.currentUserId(), k, null);
    }

    @GetMapping("/me/lanes/{projectId}/next")
    public NextTaskResponse nextMine(@PathVariable Long projectId) {
        return arrangementService.nextTask(projectId,
                authenticatedUserResolver.currentUserId(), null, null);
    }

    private static Map<TaskPriority, Double> weightOverrides(Double critical, Double high,
                                                             Double medium, Double low) {
        Map<TaskPriority, Double> overrides = new EnumMap<>(TaskPriority.class);
        if (critical != null) overrides.put(TaskPriority.CRITICAL, critical);
        if (high != null) overrides.put(TaskPriority.HIGH, high);
        if (medium != null) overrides.put(TaskPriority.MEDIUM, medium);
        if (low != null) overrides.put(TaskPriority.LOW, low);
        return overrides.isEmpty() ? null : overrides;
    }
}
