package com.roamtrip.entity.user;

import com.roamtrip.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

