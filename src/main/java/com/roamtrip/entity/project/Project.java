package com.roamtrip.entity.project;

import com.roamtrip.entity.base.BaseEntity;
import com.roamtrip.entity.enums.ProjectVisibility;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "projects",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_project_key_per_org", columnNames = {"org_id", "key"})
        },
        indexes = {
                @Index(name = "idx_project_org", columnList = "org_id")
        }
)
public class Project extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(name = "key", nullable = false, length = 32)
    private String key;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 32)
    private ProjectVisibility visibility = ProjectVisibility.PRIVATE;
}

