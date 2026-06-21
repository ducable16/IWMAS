package com.iwas.project.service;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.common.storage.FileValidator;
import com.iwas.common.storage.StorageService;
import com.iwas.project.dto.ProjectDocumentResponse;
import com.iwas.project.entity.ProjectDocument;
import com.iwas.project.repository.ProjectDocumentRepository;
import com.iwas.project.repository.ProjectRepository;
import com.iwas.security.AuthenticatedUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectDocumentService {

    private final ProjectRepository projectRepository;
    private final ProjectDocumentRepository documentRepository;
    private final StorageService storageService;
    private final FileValidator fileValidator;
    private final ProjectService projectService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public List<ProjectDocumentResponse> listDocuments(Long projectId) {
        requireProject(projectId);
        projectService.requireProjectAccess(projectId);
        return documentRepository.findByProjectId(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProjectDocumentResponse upload(Long projectId, MultipartFile file) {
        fileValidator.validateAttachment(file);
        requireProject(projectId);
        projectService.requireProjectAccess(projectId);

        String ext = resolveExtension(file.getOriginalFilename());
        String key = "projects/" + projectId + "/documents/" + UUID.randomUUID() + ext;
        storageService.upload(file, key);

        ProjectDocument doc = new ProjectDocument();
        doc.setProjectId(projectId);
        doc.setFileName(file.getOriginalFilename());
        doc.setFileKey(key);
        doc.setFileSize(file.getSize());
        doc.setContentType(file.getContentType());
        doc.setUploadedBy(authenticatedUserResolver.currentUserId());

        return toResponse(documentRepository.save(doc));
    }

    @Transactional
    public void delete(Long projectId, Long documentId) {
        requireProject(projectId);
        ProjectDocument doc = documentRepository.findById(documentId)
                .filter(d -> d.getProjectId().equals(projectId) && !Boolean.TRUE.equals(d.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        Long callerId = authenticatedUserResolver.currentUserId();
        boolean isUploader = doc.getUploadedBy().equals(callerId);
        boolean isManager = projectService.isManagerOf(projectId, callerId);

        if (!isUploader && !isManager) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        doc.setIsDeleted(true);
        documentRepository.save(doc);
        storageService.delete(doc.getFileKey());
    }

    private ProjectDocumentResponse toResponse(ProjectDocument d) {
        return ProjectDocumentResponse.builder()
                .id(d.getId())
                .projectId(d.getProjectId())
                .fileName(d.getFileName())
                .url(storageService.getUrl(d.getFileKey()))
                .fileSize(d.getFileSize())
                .contentType(d.getContentType())
                .uploadedBy(d.getUploadedBy())
                .createdAt(d.getCreatedAt())
                .build();
    }

    private void requireProject(Long projectId) {
        projectRepository.findById(projectId)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private String resolveExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.'));
        }
        return "";
    }
}
