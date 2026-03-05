package com.roamtrip.entity.workflow;

import com.roamtrip.entity.base.BaseEntity;
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

