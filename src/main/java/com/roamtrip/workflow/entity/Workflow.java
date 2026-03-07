package com.roamtrip.workflow.entity;

import com.roamtrip.common.entity.BaseEntity;
import com.roamtrip.project.entity.Project;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "workflows",
        indexes = {
                @Index(name = "idx_workflow_project", columnList = "project_id")
        }
)
public class Workflow extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "name", nullable = false, length = 255)
    private String name;
}
