package com.roamtrip.tenant;

import org.springframework.data.jpa.domain.Specification;

/**
 * Helper Specifications to enforce "always scope by org_id".
 *
 * Convention: for tables without org_id, always join through project -> organization.
 */
public final class TenantQueries {
    private TenantQueries() {
    }

    public static <T> Specification<T> requireOrgId(Long orgId, String projectPath, String orgIdPathOnProject) {
        return (root, query, cb) -> cb.equal(root.get(projectPath).get(orgIdPathOnProject), orgId);
    }
}

