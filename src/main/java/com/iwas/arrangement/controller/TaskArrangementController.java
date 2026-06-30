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
    public ArrangeResponse arrange(@PathVariable Long projectId, @PathVariable Long assigneeId) {
        return arrangementService.arrangeLane(projectId, assigneeId);
    }

    @GetMapping("/lanes/{projectId}/{assigneeId}/next")
    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    public NextTaskResponse next(@PathVariable Long projectId, @PathVariable Long assigneeId) {
        return arrangementService.nextTask(projectId, assigneeId);
    }

    @GetMapping("/me/lanes/{projectId}")
    public ArrangeResponse arrangeMine(@PathVariable Long projectId) {
        return arrangementService.arrangeLane(projectId, authenticatedUserResolver.currentUserId());
    }

    @GetMapping("/me/lanes/{projectId}/next")
    public NextTaskResponse nextMine(@PathVariable Long projectId) {
        return arrangementService.nextTask(projectId, authenticatedUserResolver.currentUserId());
    }
}
