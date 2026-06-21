package com.iwas.task.service;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.common.storage.FileValidator;
import com.iwas.common.storage.StorageService;
import com.iwas.project.service.ProjectService;
import com.iwas.security.AuthenticatedUserResolver;
import com.iwas.task.dto.TaskAttachmentResponse;
import com.iwas.task.entity.Task;
import com.iwas.task.entity.TaskActivity;
import com.iwas.task.entity.TaskAttachment;
import com.iwas.task.enums.TaskActivityType;
import com.iwas.task.repository.TaskActivityRepository;
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
    private final TaskActivityRepository taskActivityRepository;
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

        TaskAttachment saved = attachmentRepository.save(attachment);
        recordActivity(taskId, TaskActivityType.ATTACHMENT_ADDED, null, saved.getFileName());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long taskId, Long attachmentId) {
        Task task = findTask(taskId);
        TaskAttachment attachment = attachmentRepository.findById(attachmentId)
                .filter(a -> a.getTaskId().equals(taskId) && !Boolean.TRUE.equals(a.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.ATTACHMENT_NOT_FOUND));

        Long callerId = authenticatedUserResolver.currentUserId();
        boolean isUploader = attachment.getUploadedBy().equals(callerId);
        boolean isManager = projectService.isManagerOf(task.getProjectId(), callerId);

        if (!isUploader && !isManager) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        attachment.setIsDeleted(true);
        attachmentRepository.save(attachment);
        storageService.delete(attachment.getFileKey());
        recordActivity(taskId, TaskActivityType.ATTACHMENT_REMOVED, attachment.getFileName(), null);
    }

    private void recordActivity(Long taskId, TaskActivityType action, String oldValue, String newValue) {
        TaskActivity activity = new TaskActivity();
        activity.setTaskId(taskId);
        activity.setActorId(authenticatedUserResolver.currentUserId());
        activity.setAction(action);
        activity.setOldValue(oldValue);
        activity.setNewValue(newValue);
        taskActivityRepository.save(activity);
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
