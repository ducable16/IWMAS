package com.roamtrip.entity.workflow;

import com.roamtrip.entity.base.BaseEntity;
import com.roamtrip.entity.project.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

