package com.iwas.user.entity;

import com.iwas.common.entity.BaseEntity;
import com.iwas.user.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_email", columnNames = {"email"}),
                @UniqueConstraint(name = "uq_user_username", columnNames = {"username"})
        },
        indexes = {
                @Index(name = "idx_user_created_at", columnList = "created_at")
        }
)
public class User extends BaseEntity {

    @Column(name = "username", unique = true, length = 100)
    private String username;

    @Column(name = "email", nullable = false, length = 100, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "position", length = 100)
    private String position;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private UserRole role = UserRole.TEAM_MEMBER;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
