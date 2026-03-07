package com.roamtrip.workflow.entity;

import com.roamtrip.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "workflow_statuses",
        indexes = {
                @Index(name = "idx_workflow_status_workflow", columnList = "workflow_id")
        }
)
public class WorkflowStatus extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "category", length = 32)
    private String category;
}
