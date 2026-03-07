package com.roamtrip.project.entity;

import com.roamtrip.common.entity.BaseEntity;
import com.roamtrip.organization.entity.Organization;
import com.roamtrip.project.enums.ProjectVisibility;
import jakarta.persistence.*;
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
