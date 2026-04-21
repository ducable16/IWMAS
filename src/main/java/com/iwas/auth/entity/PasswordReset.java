package com.iwas.auth.entity;

import com.iwas.common.entity.BaseEntity;
import com.iwas.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "password_resets",
        indexes = {
                @Index(name = "idx_password_reset_user", columnList = "user_id"),
                @Index(name = "idx_password_reset_expires_at", columnList = "expires_at")
        }
)
public class PasswordReset extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "reset_token_hash", nullable = false, length = 255)
    private String resetTokenHash;

    @Column(name = "token_last4", length = 8)
    private String tokenLast4;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;
}
