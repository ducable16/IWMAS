package com.iwas.project.dto;

import com.iwas.project.enums.ProjectRoleInProject;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ProjectMemberResponse {
    private Long id;
    private Long projectId;
    private Long userId;
    private String userFullName;
    private ProjectRoleInProject roleInProject;
    private Integer allocatedEffortPercent;
    private LocalDate joinDate;
    private LocalDate leaveDate;
    private String note;
}
