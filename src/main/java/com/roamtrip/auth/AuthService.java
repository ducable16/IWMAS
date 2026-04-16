package com.roamtrip.auth;

import com.roamtrip.auth.dto.AuthResponse;
import com.roamtrip.auth.dto.ForgotPasswordRequest;
import com.roamtrip.auth.dto.LoginRequest;
import com.roamtrip.auth.dto.RefreshTokenRequest;
import com.roamtrip.auth.dto.RegisterRequest;
import com.roamtrip.auth.dto.ResetPasswordRequest;
import com.roamtrip.user.dto.UserMeResponse;
import com.roamtrip.common.enums.ErrorCode;
import com.roamtrip.auth.entity.EmailVerification;
import com.roamtrip.auth.entity.PasswordReset;
import com.roamtrip.user.entity.User;
import com.roamtrip.user.entity.UserSession;
import com.roamtrip.common.exception.AppException;
import com.roamtrip.auth.repository.EmailVerificationRepository;
import com.roamtrip.auth.repository.PasswordResetRepository;
import com.roamtrip.user.repository.UserRepository;
import com.roamtrip.user.repository.UserSessionRepository;
import com.roamtrip.security.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenHashingService tokenHashingService;
    private final EmailNotificationProducer emailNotificationProducer;
    private final JwtService jwtService;

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

        emailNotificationProducer.publish(EmailMessage.builder()
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
    public AuthResponse login(LoginRequest request) {
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

        UserSession session = new UserSession();
        session.setUser(user);
        session.setExpiresAt(LocalDateTime.now().plusDays(7));
        session.setRefreshTokenHash("pending");
        session = userSessionRepository.save(session);

        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail(), session.getId());
        session.setRefreshTokenHash(tokenHashingService.sha256(refreshToken));
        session.setTokenLast4(refreshToken.substring(Math.max(0, refreshToken.length() - 4)));
        userSessionRepository.save(session);

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name(), session.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getAccessExpirationSeconds())
                .user(toMeResponse(user))
                .build();
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        if (!jwtService.isTokenValid(request.getRefreshToken())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, "Invalid refresh token");
        }
        Claims claims = jwtService.parseClaims(request.getRefreshToken());
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, "Invalid token type");
        }

        UserSession session = userSessionRepository
                .findByRefreshTokenHash(tokenHashingService.sha256(request.getRefreshToken()))
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED, "Session not found"));

        if (!Boolean.TRUE.equals(session.getIsActive()) || session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, "Session expired or revoked");
        }

        User user = session.getUser();
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name(), session.getId());
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(request.getRefreshToken())
                .expiresIn(jwtService.getAccessExpirationSeconds())
                .user(toMeResponse(user))
                .build();
    }

    @Transactional
    public void logout(Long sessionId) {
        if (sessionId == null) {
            return;
        }
        userSessionRepository.findById(sessionId).ifPresent(session -> {
            session.setIsActive(false);
            userSessionRepository.save(session);
        });
    }

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

            emailNotificationProducer.publish(EmailMessage.builder()
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

        userSessionRepository.deleteByUser(user);
        reset.setUsedAt(LocalDateTime.now());
        passwordResetRepository.save(reset);
        return "Password reset successful";
    }

    public UserMeResponse toMeResponse(User user) {
        return UserMeResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .departmentId(user.getDepartmentId())
                .position(user.getPosition())
                .role(user.getRole())
                .verified(user.getIsVerified())
                .active(user.getIsActive())
                .build();
    }
}
