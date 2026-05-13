package com.iwas.project.repository;

import com.iwas.project.entity.ProjectDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, Long> {

    @Query("SELECT d FROM ProjectDocument d WHERE d.projectId = :projectId AND d.isDeleted = false ORDER BY d.createdAt DESC")
    List<ProjectDocument> findByProjectId(Long projectId);
}
