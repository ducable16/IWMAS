package com.roamtrip.security;

/**
 * Optional bridge interface for Spring Security principals that expose a stable user id.
 * Implement this in your authenticated principal to enable JPA auditing for createdBy/updatedBy.
 */
public interface UserIdPrincipal {
    Long getUserId();
}

