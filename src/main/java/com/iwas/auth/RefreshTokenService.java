package com.iwas.auth;

import com.iwas.auth.entity.RefreshToken;
import com.iwas.auth.repository.RefreshTokenRepository;
import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.user.entity.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    public static final String COOKIE_NAME = "refresh_token";
    public static final String COOKIE_PATH = "/api/auth";
    private static final Duration REFRESH_TTL = Duration.ofDays(7);

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.cookies.secure:true}")
    private boolean cookieSecure;

    public IssuedToken issue(User user) {
        String rawToken = UUID.randomUUID().toString();
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(DigestUtils.sha256Hex(rawToken));
        entity.setExpiresAt(LocalDateTime.now().plus(REFRESH_TTL));
        entity.setRevoked(false);
        refreshTokenRepository.save(entity);
        return new IssuedToken(rawToken, REFRESH_TTL);
    }

    @Transactional
    public RotationResult rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_MISSING);
        }
        RefreshToken existing = refreshTokenRepository.findByTokenHash(DigestUtils.sha256Hex(rawToken))
                .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_INVALID));

        if (Boolean.TRUE.equals(existing.getRevoked())) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_REVOKED);
        }
        if (existing.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        IssuedToken next = issue(existing.getUser());
        return new RotationResult(existing.getUser(), next);
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(DigestUtils.sha256Hex(rawToken))
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }

    public ResponseCookie buildCookie(String rawToken, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, rawToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }

    public ResponseCookie buildClearCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        int deleted = refreshTokenRepository.deleteAllExpired(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Purged {} expired refresh tokens", deleted);
        }
    }

    public record IssuedToken(String rawToken, Duration ttl) {}

    public record RotationResult(User user, IssuedToken token) {}
}