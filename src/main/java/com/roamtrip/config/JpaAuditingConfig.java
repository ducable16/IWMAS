package com.roamtrip.config;

import com.roamtrip.security.UserIdPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }

            Object principal = authentication.getPrincipal();
            if (principal == null) {
                return Optional.empty();
            }

            if (principal instanceof UserIdPrincipal userIdPrincipal) {
                return Optional.ofNullable(userIdPrincipal.getUserId());
            }

            if (principal instanceof Long userId) {
                return Optional.of(userId);
            }

            if (principal instanceof String s) {
                try {
                    return Optional.of(Long.parseLong(s));
                } catch (NumberFormatException ignored) {
                    return Optional.empty();
                }
            }

            return Optional.empty();
        };
    }
}

