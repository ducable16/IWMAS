package com.roamtrip.workflow.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.roamtrip.common.entity.BaseEntity;
import com.roamtrip.workflow.enums.WorkflowTransitionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(
        name = "workflow_transitions",
        indexes = {
                @Index(name = "idx_transition_workflow", columnList = "workflow_id"),
                @Index(name = "idx_transition_from_to", columnList = "from_status_id,to_status_id")
        }
)
public class WorkflowTransition extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_status_id", nullable = false)
    private WorkflowStatus fromStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_status_id", nullable = false)
    private WorkflowStatus toStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "transition_type", nullable = false, length = 32)
    private WorkflowTransitionType transitionType = WorkflowTransitionType.NORMAL;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "condition_expr", columnDefinition = "jsonb")
    private JsonNode conditionExpr;
}
