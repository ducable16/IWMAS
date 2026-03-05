package com.roamtrip.entity;

import com.roamtrip.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "tenants",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_tenant_key", columnNames = {"key"})
        }
)
public class Tenant extends BaseEntity {

    @Column(name = "key", nullable = false, length = 64)
    private String key;

    @Column(name = "name", nullable = false, length = 255)
    private String name;
}
