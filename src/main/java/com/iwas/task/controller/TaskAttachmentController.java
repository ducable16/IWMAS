package com.iwas.task.controller;

import com.iwas.task.dto.TaskAttachmentResponse;
import com.iwas.task.service.TaskAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/attachments")
@RequiredArgsConstructor
public class TaskAttachmentController {

    private final TaskAttachmentService attachmentService;

    @GetMapping
    public List<TaskAttachmentResponse> list(@PathVariable Long taskId) {
        return attachmentService.listAttachments(taskId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskAttachmentResponse upload(@PathVariable Long taskId,
                                         @RequestParam("file") MultipartFile file) {
        return attachmentService.upload(taskId, file);
    }

    @DeleteMapping("/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long taskId, @PathVariable Long attachmentId) {
        attachmentService.delete(taskId, attachmentId);
    }
}
