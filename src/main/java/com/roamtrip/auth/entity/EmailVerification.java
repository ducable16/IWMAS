package com.roamtrip.auth.entity;

import com.roamtrip.common.entity.BaseEntity;
import com.roamtrip.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "email_verifications",
        indexes = {
                @Index(name = "idx_email_verification_user", columnList = "user_id"),
                @Index(name = "idx_email_verification_expires_at", columnList = "expires_at")
        }
)
public class EmailVerification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "verification_token_hash", nullable = false, length = 255)
    private String verificationTokenHash;

    @Column(name = "token_last4", length = 8)
    private String tokenLast4;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
}
