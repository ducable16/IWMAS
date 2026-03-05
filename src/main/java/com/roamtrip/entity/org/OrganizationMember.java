package com.roamtrip.entity.org;

import com.roamtrip.entity.base.BaseEntity;
import com.roamtrip.entity.enums.OrgRole;
import com.roamtrip.entity.user.User;
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

