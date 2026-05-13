package com.iwas.project.controller;

import com.iwas.project.dto.ProjectDocumentResponse;
import com.iwas.project.service.ProjectDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/documents")
@RequiredArgsConstructor
public class ProjectDocumentController {

    private final ProjectDocumentService documentService;

    @GetMapping
    public List<ProjectDocumentResponse> list(@PathVariable Long projectId) {
        return documentService.listDocuments(projectId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDocumentResponse upload(@PathVariable Long projectId,
                                          @RequestParam("file") MultipartFile file) {
        return documentService.upload(projectId, file);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long projectId, @PathVariable Long documentId) {
        documentService.delete(projectId, documentId);
    }
}
