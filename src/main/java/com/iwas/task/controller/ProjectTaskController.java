package com.iwas.task.controller;

import com.iwas.task.dto.TaskResponse;
import com.iwas.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
@RequiredArgsConstructor
public class ProjectTaskController {

    private final TaskService taskService;

    @GetMapping
    public List<TaskResponse> getByProject(@PathVariable Long projectId) {
        return taskService.getTasksByProject(projectId);
    }
}
