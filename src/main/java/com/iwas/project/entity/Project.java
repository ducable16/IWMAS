package com.iwas.project.entity;

import com.iwas.common.entity.BaseEntity;
import com.iwas.project.enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(
        name = "projects",
        indexes = {
                @Index(name = "idx_project_manager", columnList = "manager_id"),
                @Index(name = "idx_project_status", columnList = "status")
        }
)
public class Project extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", length = 10, updatable = false)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ProjectStatus status = ProjectStatus.PLANNING;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Column(name = "manager_id", nullable = false)
    private Long managerId;
}
