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
        name = "otp_verifications",
        indexes = {
                @Index(name = "idx_otp_verification_user", columnList = "user_id"),
                @Index(name = "idx_otp_verification_expires_at", columnList = "expires_at")
        }
)
public class OtpVerification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "otp_hash", nullable = false, length = 255)
    private String otpHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
}
