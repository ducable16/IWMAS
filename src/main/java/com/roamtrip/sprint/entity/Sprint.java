package com.roamtrip.sprint.entity;

import com.roamtrip.common.entity.BaseEntity;
import com.roamtrip.project.entity.Project;
import com.roamtrip.sprint.enums.SprintStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(
        name = "sprints",
        indexes = {
                @Index(name = "idx_sprint_project", columnList = "project_id"),
                @Index(name = "idx_sprint_status", columnList = "project_id,status")
        }
)
public class Sprint extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SprintStatus status = SprintStatus.PLANNED;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;
}
