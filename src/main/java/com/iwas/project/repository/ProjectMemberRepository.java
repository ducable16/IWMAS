package com.iwas.project.repository;

import com.iwas.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.isDeleted = false AND pm.projectId = :projectId")
    List<ProjectMember> findByProjectId(Long projectId);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.isDeleted = false AND pm.userId = :userId AND pm.leaveDate IS NULL")
    List<ProjectMember> findActiveProjectsByUserId(Long userId);

    Optional<ProjectMember> findByProjectIdAndUserIdAndIsDeletedFalse(Long projectId, Long userId);
}
