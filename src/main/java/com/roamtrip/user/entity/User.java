package com.roamtrip.user.entity;

import com.roamtrip.common.entity.BaseEntity;
import com.roamtrip.common.enums.Gender;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_email", columnNames = {"email"})
        },
        indexes = {
                @Index(name = "idx_user_created_at", columnList = "created_at")
        }
)
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 32)
    private Gender gender;
}
