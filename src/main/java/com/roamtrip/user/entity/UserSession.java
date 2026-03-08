package com.roamtrip.user.entity;

import com.roamtrip.common.entity.BaseEntity;
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
        name = "user_sessions",
        indexes = {
                @Index(name = "idx_user_session_user", columnList = "user_id"),
                @Index(name = "idx_user_session_expires_at", columnList = "expires_at")
        }
)
public class UserSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "refresh_token_hash", nullable = false, length = 255)
    private String refreshTokenHash;

    @Column(name = "token_last4", length = 8)
    private String tokenLast4;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
