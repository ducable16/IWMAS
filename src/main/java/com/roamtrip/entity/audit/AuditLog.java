package com.roamtrip.entity.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.roamtrip.entity.base.BaseEntity;
import com.roamtrip.entity.enums.AuditAction;
import com.roamtrip.entity.org.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_org_created_at", columnList = "org_id,created_at"),
                @Index(name = "idx_audit_entity_lookup", columnList = "org_id,entity_type,entity_id,created_at")
        }
)
public class AuditLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 32)
    private AuditAction action;

    @Column(name = "entity_type", nullable = false, length = 128)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value", columnDefinition = "jsonb")
    private JsonNode oldValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", columnDefinition = "jsonb")
    private JsonNode newValue;
}

