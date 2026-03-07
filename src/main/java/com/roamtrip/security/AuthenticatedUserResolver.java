package com.roamtrip.security;

import com.roamtrip.common.enums.ErrorCode;
import com.roamtrip.common.exception.AppException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserResolver {

    public Long currentUserId() {
        Object principal = principal();
        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    public Long currentSessionId() {
        Object principal = principal();
        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getSessionId();
        }
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    private Object principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return authentication.getPrincipal();
    }
}
