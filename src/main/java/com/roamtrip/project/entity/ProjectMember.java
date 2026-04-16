package com.roamtrip.project.entity;

import com.roamtrip.common.entity.BaseEntity;
import com.roamtrip.project.enums.ProjectRoleInProject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(
        name = "project_members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_project_member", columnNames = {"project_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_pm_project", columnList = "project_id"),
                @Index(name = "idx_pm_user", columnList = "user_id")
        }
)
public class ProjectMember extends BaseEntity {

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_in_project", nullable = false, length = 50)
    private ProjectRoleInProject roleInProject = ProjectRoleInProject.MEMBER;

    @Column(name = "allocated_effort_percent")
    private Integer allocatedEffortPercent;

    @Column(name = "join_date")
    private LocalDate joinDate;

    @Column(name = "leave_date")
    private LocalDate leaveDate;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
