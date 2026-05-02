package com.iwas.auth.service;

import com.iwas.auth.*;
import com.iwas.auth.dto.AuthResponse;
import com.iwas.auth.dto.ForgotPasswordRequest;
import com.iwas.auth.dto.LoginRequest;
import com.iwas.auth.dto.RegisterRequest;
import com.iwas.auth.dto.ResetPasswordRequest;
import com.iwas.auth.dto.SendOtpRequest;
import com.iwas.auth.dto.VerifyOtpRequest;
import com.iwas.auth.entity.*;
import com.iwas.common.mesaging.publisher.EmailNotificationPublisher;
import com.iwas.user.dto.UserMeResponse;
import com.iwas.common.enums.ErrorCode;
import com.iwas.user.entity.User;
import com.iwas.common.exception.AppException;
import com.iwas.auth.repository.EmailVerificationRepository;
import com.iwas.auth.repository.OtpVerificationRepository;
import com.iwas.auth.repository.PasswordResetRepository;
import com.iwas.user.repository.UserRepository;
import com.iwas.security.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final SessionStore sessionStore;
    private final PasswordEncoder passwordEncoder;
    private final TokenHashingService tokenHashingService;
    private final EmailNotificationPublisher emailNotificationPublisher;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    private static final int OTP_MAX_ATTEMPTS = 5;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String register(RegisterRequest request) {
        userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .ifPresent(u -> {
                    throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
                });

        User user = new User();
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getName().trim());
        user.setIsVerified(false);
        user = userRepository.save(user);

        String rawToken = UUID.randomUUID().toString();
        EmailVerification verification = new EmailVerification();
        verification.setUser(user);
        verification.setVerificationTokenHash(tokenHashingService.sha256(rawToken));
        verification.setTokenLast4(rawToken.substring(Math.max(0, rawToken.length() - 4)));
        verification.setExpiresAt(LocalDateTime.now().plusHours(24));
        emailVerificationRepository.save(verification);

        emailNotificationPublisher.publish(EmailMessage.builder()
                .to(user.getEmail())
                .subject("Verify your Workforce account")
                .template("email-verification")
                .token(rawToken)
                .build());

        return "Check your email to verify your account";
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerification verification = emailVerificationRepository
                .findByVerificationTokenHash(tokenHashingService.sha256(token))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_INPUT, "Invalid verification token"));

        if (verification.getVerifiedAt() != null || verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Verification token expired or used");
        }

        verification.setVerifiedAt(LocalDateTime.now());
        User user = verification.getUser();
        user.setIsVerified(true);
        userRepository.save(user);
        emailVerificationRepository.save(verification);
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Email not verified. Please check your inbox.");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Account is deactivated. Contact your administrator.");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.WRONG_PASSWORD);
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        SessionEntry session = sessionStore.create(user, LocalDateTime.now().plusDays(7));
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name(), session.getId());

        RefreshTokenService.IssuedToken refresh = refreshTokenService.issue(user);

        AuthResponse response = AuthResponse.builder()
                .accessToken(accessToken)
                .expiresIn(jwtService.getAccessExpirationSeconds())
                .user(toMeResponse(user))
                .build();
        return new LoginResult(response, refresh);
    }

    @Transactional
    public RefreshResult refresh(String rawRefreshToken) {
        RefreshTokenService.RotationResult rotated = refreshTokenService.rotate(rawRefreshToken);
        User user = rotated.user();

        SessionEntry session = sessionStore.create(user, LocalDateTime.now().plusDays(7));
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name(), session.getId());

        AuthResponse response = AuthResponse.builder()
                .accessToken(accessToken)
                .expiresIn(jwtService.getAccessExpirationSeconds())
                .user(toMeResponse(user))
                .build();
        return new RefreshResult(response, rotated.token());
    }

    public void logout(Long sessionId, String rawRefreshToken) {
        if (sessionId != null) {
            sessionStore.deactivate(sessionId);
        }
        refreshTokenService.revoke(rawRefreshToken);
    }

    public record LoginResult(AuthResponse response, RefreshTokenService.IssuedToken refreshToken) {}

    public record RefreshResult(AuthResponse response, RefreshTokenService.IssuedToken refreshToken) {}

    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail().trim().toLowerCase()).ifPresent(user -> {
            String rawToken = UUID.randomUUID().toString();
            PasswordReset reset = new PasswordReset();
            reset.setUser(user);
            reset.setResetTokenHash(tokenHashingService.sha256(rawToken));
            reset.setTokenLast4(rawToken.substring(Math.max(0, rawToken.length() - 4)));
            reset.setExpiresAt(LocalDateTime.now().plusHours(1));
            passwordResetRepository.save(reset);

            emailNotificationPublisher.publish(EmailMessage.builder()
                    .to(user.getEmail())
                    .subject("Reset your Workforce password")
                    .template("password-reset")
                    .token(rawToken)
                    .build());
        });
        return "If the email exists, a reset instruction has been sent";
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        PasswordReset reset = passwordResetRepository
                .findByResetTokenHash(tokenHashingService.sha256(request.getToken()))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_INPUT, "Invalid reset token"));

        if (reset.getUsedAt() != null || reset.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Reset token expired or used");
        }

        User user = reset.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        sessionStore.deleteByUser(user.getId());
        reset.setUsedAt(LocalDateTime.now());
        passwordResetRepository.save(reset);
        return "Password reset successful";
    }

    @Transactional
    public String sendOtp(SendOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        otpVerificationRepository.deleteByUser(user);

        String rawOtp = String.format("%06d", secureRandom.nextInt(1_000_000));
        OtpVerification otpVerification = new OtpVerification();
        otpVerification.setUser(user);
        otpVerification.setOtpHash(tokenHashingService.sha256(rawOtp));
        otpVerification.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        otpVerificationRepository.save(otpVerification);

        emailNotificationPublisher.publish(EmailMessage.builder()
                .to(user.getEmail())
                .subject("Your Workforce verification code")
                .template("email-otp")
                .token(rawOtp)
                .build());

        return "OTP has been sent to your email";
    }

    @Transactional
    public String verifyEmailOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        OtpVerification otpRecord = otpVerificationRepository
                .findTopByUserOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_OTP));

        if (otpRecord.getVerifiedAt() != null) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }
        if (otpRecord.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }
        if (otpRecord.getAttemptCount() >= OTP_MAX_ATTEMPTS) {
            throw new AppException(ErrorCode.OTP_EXPIRED, "OTP invalidated after too many failed attempts");
        }
        if (!otpRecord.getOtpHash().equals(tokenHashingService.sha256(request.getOtp()))) {
            otpRecord.setAttemptCount(otpRecord.getAttemptCount() + 1);
            otpVerificationRepository.save(otpRecord);
            throw new AppException(ErrorCode.OTP_INCORRECT);
        }

        otpRecord.setVerifiedAt(LocalDateTime.now());
        otpVerificationRepository.save(otpRecord);

        user.setIsVerified(true);
        userRepository.save(user);

        return "Email verified successfully";
    }

    public UserMeResponse toMeResponse(User user) {
        return UserMeResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .position(user.getPosition())
                .role(user.getRole())
                .verified(user.getIsVerified())
                .active(user.getIsActive())
                .build();
    }
}
