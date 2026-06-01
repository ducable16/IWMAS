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

    // "Active" = not soft-deleted and not yet left. A null leaveDate means "stays until the project
    // completes"; a leaveDate set in the future means still active up to and including that day.
    // Hence leaveDate IS NULL OR leaveDate >= CURRENT_DATE — a member with a future leaveDate must
    // not be treated as already gone.
    @Query("SELECT pm FROM ProjectMember pm WHERE pm.isDeleted = false AND pm.projectId = :projectId AND (pm.leaveDate IS NULL OR pm.leaveDate >= CURRENT_DATE)")
    List<ProjectMember> findActiveMembersByProjectId(Long projectId);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.isDeleted = false AND pm.userId = :userId AND (pm.leaveDate IS NULL OR pm.leaveDate >= CURRENT_DATE)")
    List<ProjectMember> findActiveProjectsByUserId(Long userId);

    /**
     * All non-deleted memberships of a user, regardless of leaveDate. Used by capacity
     * calculations that bound each allocation by its own join/leave window — a membership
     * with a future leaveDate is still consuming capacity until then, so it must not be
     * filtered out the way {@link #findActiveProjectsByUserId} does.
     */
    @Query("SELECT pm FROM ProjectMember pm WHERE pm.isDeleted = false AND pm.userId = :userId")
    List<ProjectMember> findByUserIdAndIsDeletedFalse(Long userId);

    Optional<ProjectMember> findByProjectIdAndUserIdAndIsDeletedFalse(Long projectId, Long userId);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.isDeleted = false AND pm.projectId = :projectId AND pm.userId = :userId AND (pm.leaveDate IS NULL OR pm.leaveDate >= CURRENT_DATE)")
    Optional<ProjectMember> findActiveMemberByProjectIdAndUserId(@Param("projectId") Long projectId, @Param("userId") Long userId);
}
