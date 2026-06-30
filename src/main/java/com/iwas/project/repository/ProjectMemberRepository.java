package com.iwas.project.repository;

import com.iwas.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.isDeleted = false AND pm.projectId = :projectId")
    List<ProjectMember> findByProjectId(Long projectId);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.isDeleted = false AND pm.projectId = :projectId AND (pm.leaveDate IS NULL OR pm.leaveDate >= CURRENT_DATE)")
    List<ProjectMember> findActiveMembersByProjectId(Long projectId);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.isDeleted = false AND pm.projectId IN :projectIds AND (pm.leaveDate IS NULL OR pm.leaveDate >= CURRENT_DATE)")
    List<ProjectMember> findActiveMembersByProjectIdIn(@Param("projectIds") List<Long> projectIds);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.isDeleted = false AND pm.userId = :userId AND (pm.leaveDate IS NULL OR pm.leaveDate >= CURRENT_DATE)")
    List<ProjectMember> findActiveProjectsByUserId(Long userId);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.isDeleted = false AND pm.userId = :userId")
    List<ProjectMember> findByUserIdAndIsDeletedFalse(Long userId);

    Optional<ProjectMember> findByProjectIdAndUserIdAndIsDeletedFalse(Long projectId, Long userId);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.isDeleted = false AND pm.projectId = :projectId AND pm.userId = :userId AND (pm.leaveDate IS NULL OR pm.leaveDate >= CURRENT_DATE)")
    Optional<ProjectMember> findActiveMemberByProjectIdAndUserId(@Param("projectId") Long projectId, @Param("userId") Long userId);
}
