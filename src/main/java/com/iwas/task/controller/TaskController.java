package com.iwas.task.controller;

import com.iwas.security.AuthenticatedUserResolver;
import com.iwas.task.dto.*;
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
import java.util.Map;
import java.util.Set;

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
            @RequestParam(required = false) List<String> labels,
            @RequestParam(required = false) String sprint,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateTo,
            @RequestParam(required = false) Map<String, String> customFields,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        // Remove known params from customFields map so only actual custom fields remain
        Set<String> knownParams = Set.of("search", "projectId", "statuses", "assigneeId", "reporterId",
                "priorities", "types", "labels", "sprint", "dueDateFrom", "dueDateTo",
                "sortBy", "sortDirection", "page", "size");
        Map<String, String> onlyCustomFields = customFields == null ? null :
                customFields.entrySet().stream()
                        .filter(e -> !knownParams.contains(e.getKey()))
                        .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        TaskFilterRequest filter = new TaskFilterRequest();
        filter.setSearch(search);
        filter.setProjectId(projectId);
        filter.setStatuses(statuses);
        filter.setAssigneeId(assigneeId);
        filter.setReporterId(reporterId);
        filter.setPriorities(priorities);
        filter.setTypes(types);
        filter.setLabels(labels);
        filter.setSprint(sprint);
        filter.setDueDateFrom(dueDateFrom);
        filter.setDueDateTo(dueDateTo);
        filter.setCustomFields(onlyCustomFields != null && !onlyCustomFields.isEmpty() ? onlyCustomFields : null);
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
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody TaskRequest request) {
        return taskService.createTask(request, authenticatedUserResolver.currentUserId());
    }

    @PostMapping("/{id}/update")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public TaskResponse update(@PathVariable Long id,
                               @Valid @RequestBody TaskRequest request) {
        return taskService.updateTask(id, request);
    }

    @PostMapping("/{id}/status")
    public TaskResponse updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody TaskStatusUpdateRequest request) {
        return taskService.updateTaskStatus(id, request, authenticatedUserResolver.currentUserId());
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
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
    public List<TaskStatusHistoryResponse> getStatusHistory(@PathVariable Long id) {
        return taskService.getStatusHistory(id);
    }
}
