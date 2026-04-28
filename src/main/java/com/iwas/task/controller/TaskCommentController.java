package com.iwas.task.controller;

import com.iwas.security.AuthenticatedUserResolver;
import com.iwas.task.dto.TaskCommentRequest;
import com.iwas.task.dto.TaskCommentResponse;
import com.iwas.task.service.TaskCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
@RequiredArgsConstructor
public class TaskCommentController {

    private final TaskCommentService taskCommentService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @GetMapping
    public List<TaskCommentResponse> getComments(@PathVariable Long taskId) {
        return taskCommentService.getCommentsByTask(taskId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskCommentResponse addComment(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskCommentRequest request) {
        return taskCommentService.addComment(taskId, request, authenticatedUserResolver.currentUserId());
    }

    @PutMapping("/{commentId}")
    public TaskCommentResponse updateComment(
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            @Valid @RequestBody TaskCommentRequest request) {
        return taskCommentService.updateComment(commentId, request, authenticatedUserResolver.currentUserId());
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable Long taskId,
            @PathVariable Long commentId) {
        taskCommentService.deleteComment(commentId, authenticatedUserResolver.currentUserId());
    }
}
