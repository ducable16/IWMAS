package com.roamtrip.tenant;

/**
 * Request-scoped tenant holder (orgId) using ThreadLocal.
 *
 * This is a lightweight hook to enforce tenant scoping at the repository/service layer.
 * In real apps, set/clear this in a servlet filter once authentication is established.
 */
public final class TenantContext {
    private static final ThreadLocal<Long> ORG_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setOrgId(Long orgId) {
        ORG_ID.set(orgId);
    }

    public static Long getOrgId() {
        return ORG_ID.get();
    }

    public static void clear() {
        ORG_ID.remove();
    }
}

