package com.iwas.task.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskAttachmentResponse {
    private Long id;
    private Long taskId;
    private String fileName;
    private String url;
    private Long fileSize;
    private String contentType;
    private Long uploadedBy;
    private LocalDateTime createdAt;
}
