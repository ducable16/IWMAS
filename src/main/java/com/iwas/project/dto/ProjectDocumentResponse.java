package com.iwas.project.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectDocumentResponse {
    private Long id;
    private Long projectId;
    private String fileName;
    private String url;
    private Long fileSize;
    private String contentType;
    private Long uploadedBy;
    private LocalDateTime createdAt;
}
