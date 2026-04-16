package com.roamtrip.project.entity;

import com.roamtrip.common.entity.BaseEntity;
import com.roamtrip.project.enums.ProjectPriority;
import com.roamtrip.project.enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(
        name = "projects",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_project_code", columnNames = {"code"})
        },
        indexes = {
                @Index(name = "idx_project_manager", columnList = "manager_id"),
                @Index(name = "idx_project_status", columnList = "status")
        }
)
public class Project extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", unique = true, length = 50)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ProjectStatus status = ProjectStatus.PLANNING;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20)
    private ProjectPriority priority = ProjectPriority.MEDIUM;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Column(name = "manager_id", nullable = false)
    private Long managerId;
}
