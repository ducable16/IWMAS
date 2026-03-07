package com.roamtrip.project.entity;

import com.roamtrip.common.entity.BaseEntity;
import com.roamtrip.project.enums.ProjectRole;
import com.roamtrip.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "project_members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_project_member", columnNames = {"project_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_project_member_project", columnList = "project_id"),
                @Index(name = "idx_project_member_user", columnList = "user_id")
        }
)
public class ProjectMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private ProjectRole role;
}
