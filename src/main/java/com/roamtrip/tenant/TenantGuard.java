package com.roamtrip.tenant;

import com.roamtrip.entity.issue.Issue;
import com.roamtrip.entity.project.Project;

/**
 * Small enforcement hook for service layer:
 * - Always compare requested orgId with the entity's derived org id.
 */
public final class TenantGuard {
    private TenantGuard() {
    }

    public static void assertProjectInOrg(Project project, Long orgId) {
        if (project == null || project.getOrganization() == null || project.getOrganization().getId() == null) {
            throw new IllegalStateException("Project organization is not loaded");
        }
        if (!project.getOrganization().getId().equals(orgId)) {
            throw new SecurityException("Cross-tenant access denied");
        }
    }

    public static void assertIssueInOrg(Issue issue, Long orgId) {
        if (issue == null || issue.getProject() == null) {
            throw new IllegalStateException("Issue project is not loaded");
        }
        assertProjectInOrg(issue.getProject(), orgId);
    }
}

