package com.roamtrip.organization.entity;

import com.roamtrip.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "organizations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_org_key", columnNames = {"key"})
        },
        indexes = {
                @Index(name = "idx_org_created_at", columnList = "created_at")
        }
)
public class Organization extends BaseEntity {

    @Column(name = "key", nullable = false, length = 64)
    private String key;

    @Column(name = "name", nullable = false, length = 255)
    private String name;
}
