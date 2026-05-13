package com.iwas.task.service;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.common.storage.FileValidator;
import com.iwas.common.storage.StorageService;
import com.iwas.project.service.ProjectService;
import com.iwas.security.AuthenticatedUserResolver;
import com.iwas.task.dto.TaskAttachmentResponse;
import com.iwas.task.entity.Task;
import com.iwas.task.entity.TaskAttachment;
import com.iwas.task.repository.TaskAttachmentRepository;
import com.iwas.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskAttachmentService {

    private final TaskRepository taskRepository;
    private final TaskAttachmentRepository attachmentRepository;
    private final StorageService storageService;
    private final FileValidator fileValidator;
    private final ProjectService projectService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public List<TaskAttachmentResponse> listAttachments(Long taskId) {
        Task task = findTask(taskId);
        projectService.requireProjectAccess(task.getProjectId());
        return attachmentRepository.findByTaskId(taskId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TaskAttachmentResponse upload(Long taskId, MultipartFile file) {
        fileValidator.validateAttachment(file);
        Task task = findTask(taskId);
        projectService.requireProjectAccess(task.getProjectId());

        String ext = resolveExtension(file.getOriginalFilename());
        String key = "tasks/" + taskId + "/attachments/" + UUID.randomUUID() + ext;
        storageService.upload(file, key);

        TaskAttachment attachment = new TaskAttachment();
        attachment.setTaskId(taskId);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileKey(key);
        attachment.setFileSize(file.getSize());
        attachment.setContentType(file.getContentType());
        attachment.setUploadedBy(authenticatedUserResolver.currentUserId());

        return toResponse(attachmentRepository.save(attachment));
    }

    @Transactional
    public void delete(Long taskId, Long attachmentId) {
        Task task = findTask(taskId);
        TaskAttachment attachment = attachmentRepository.findById(attachmentId)
                .filter(a -> a.getTaskId().equals(taskId) && !Boolean.TRUE.equals(a.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.ATTACHMENT_NOT_FOUND));

        Long callerId = authenticatedUserResolver.currentUserId();
        String role = authenticatedUserResolver.currentUserRole();
        boolean isUploader = attachment.getUploadedBy().equals(callerId);
        boolean isManagerOrAdmin = "ADMIN".equals(role) || projectService.isManagerOf(task.getProjectId(), callerId);

        if (!isUploader && !isManagerOrAdmin) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        attachment.setIsDeleted(true);
        attachmentRepository.save(attachment);
        storageService.delete(attachment.getFileKey());
    }

    private TaskAttachmentResponse toResponse(TaskAttachment a) {
        return TaskAttachmentResponse.builder()
                .id(a.getId())
                .taskId(a.getTaskId())
                .fileName(a.getFileName())
                .url(storageService.getUrl(a.getFileKey()))
                .fileSize(a.getFileSize())
                .contentType(a.getContentType())
                .uploadedBy(a.getUploadedBy())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private Task findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));
    }

    private String resolveExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.'));
        }
        return "";
    }
}
