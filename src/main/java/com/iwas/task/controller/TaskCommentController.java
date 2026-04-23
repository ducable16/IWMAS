package com.iwas.task.controller;

import com.iwas.security.AuthenticatedUserResolver;
import com.iwas.task.dto.TaskCommentRequest;
import com.iwas.task.dto.TaskCommentResponse;
import com.iwas.task.service.TaskCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
@RequiredArgsConstructor
public class TaskCommentController {

    private final TaskCommentService taskCommentService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @GetMapping
    public ResponseEntity<List<TaskCommentResponse>> getComments(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskCommentService.getCommentsByTask(taskId));
    }

    @PostMapping
    public ResponseEntity<TaskCommentResponse> addComment(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskCommentService.addComment(taskId, request, authenticatedUserResolver.currentUserId()));
    }

    @PostMapping("/{commentId}/update")
    public ResponseEntity<TaskCommentResponse> updateComment(
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            @Valid @RequestBody TaskCommentRequest request) {
        return ResponseEntity.ok(
                taskCommentService.updateComment(commentId, request, authenticatedUserResolver.currentUserId()));
    }

    @PostMapping("/{commentId}/delete")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long taskId,
            @PathVariable Long commentId) {
        taskCommentService.deleteComment(commentId, authenticatedUserResolver.currentUserId());
        return ResponseEntity.ok().build();
    }
}
