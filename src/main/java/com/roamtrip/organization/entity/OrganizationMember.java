package com.roamtrip.organization.entity;

import com.roamtrip.common.entity.BaseEntity;
import com.roamtrip.organization.enums.OrgRole;
import com.roamtrip.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "organization_members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_org_member", columnNames = {"org_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_org_member_org", columnList = "org_id"),
                @Index(name = "idx_org_member_user", columnList = "user_id")
        }
)
public class OrganizationMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private OrgRole role;
}
