package com.roamtrip.entity.org;

import com.roamtrip.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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

