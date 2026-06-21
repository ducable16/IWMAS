package com.iwas.task.controller;

import com.iwas.security.AuthenticatedUserResolver;
import com.iwas.task.dto.*;
import com.iwas.user.dto.UserMeResponse;
import com.iwas.task.enums.TaskPriority;
import com.iwas.task.enums.TaskStatus;
import com.iwas.task.enums.TaskType;
import com.iwas.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @GetMapping
    public TaskPageResponse searchTasks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) List<TaskStatus> statuses,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Long reporterId,
            @RequestParam(required = false) List<TaskPriority> priorities,
            @RequestParam(required = false) List<TaskType> types,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateTo,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        TaskFilterRequest filter = new TaskFilterRequest();
        filter.setSearch(search);
        filter.setProjectId(projectId);
        filter.setStatuses(statuses);
        filter.setAssigneeId(assigneeId);
        filter.setReporterId(reporterId);
        filter.setPriorities(priorities);
        filter.setTypes(types);
        filter.setDueDateFrom(dueDateFrom);
        filter.setDueDateTo(dueDateTo);
        filter.setSortBy(sortBy);
        filter.setSortDirection(sortDirection);
        filter.setPage(page);
        filter.setSize(size);
        return taskService.searchTasks(filter);
    }

    @GetMapping("/board")
    public KanbanBoardResponse getKanbanBoard(@RequestParam Long projectId) {
        return taskService.getKanbanBoard(projectId);
    }

    @GetMapping("/my")
    public List<TaskResponse> getMyTasks() {
        return taskService.getMyTasks(authenticatedUserResolver.currentUserId());
    }

    @GetMapping("/{id}")
    public TaskResponse getById(@PathVariable Long id) {
        return taskService.getTaskById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody TaskRequest request) {
        return taskService.createTask(request, authenticatedUserResolver.currentUserId());
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id,
                               @Valid @RequestBody TaskRequest request) {
        return taskService.updateTask(id, request);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody TaskStatusUpdateRequest request) {
        return taskService.updateTaskStatus(id, request, authenticatedUserResolver.currentUserId());
    }

    @PatchMapping("/{id}/dates")
    public TaskResponse updateDates(@PathVariable Long id,
                                    @RequestBody TaskDateUpdateRequest request) {
        return taskService.updateTaskDates(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        taskService.deleteTask(id);
    }

    @GetMapping("/calendar")
    public List<CalendarDayResponse> getCalendarView(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long projectId
    ) {
        return taskService.getCalendarView(from, to, projectId);
    }

    @GetMapping("/{id}/history")
    public List<TaskActivityResponse> getHistory(@PathVariable Long id) {
        return taskService.getTaskActivity(id);
    }

    @GetMapping("/{id}/assignee-candidates")
    public List<UserMeResponse> getAssigneeCandidates(
            @PathVariable Long id,
            @RequestParam(defaultValue = "") String q) {
        return taskService.getAssigneeCandidates(id, q);
    }
}
