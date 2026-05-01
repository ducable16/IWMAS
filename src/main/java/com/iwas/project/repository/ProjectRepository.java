package com.iwas.project.repository;

import com.iwas.project.entity.Project;
import com.iwas.project.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    @Query("SELECT p FROM Project p WHERE p.isDeleted = false ORDER BY p.createdAt DESC")
    List<Project> findAllActive();

    @Query("SELECT p FROM Project p WHERE p.isDeleted = false AND p.status = :status ORDER BY p.createdAt DESC")
    List<Project> findByStatus(ProjectStatus status);

    @Query("SELECT p FROM Project p WHERE p.isDeleted = false AND p.managerId = :managerId ORDER BY p.createdAt DESC")
    List<Project> findByManagerId(Long managerId);

    Optional<Project> findByCodeAndIsDeletedFalse(String code);

    @Query("SELECT p FROM Project p WHERE p.isDeleted = false AND p.id IN :ids ORDER BY p.createdAt DESC")
    List<Project> findActiveByIds(List<Long> ids);
}
