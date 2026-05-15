package com.iwas.project.dto;

import com.iwas.project.enums.ProjectRoleInProject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectMemberRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    private ProjectRoleInProject roleInProject = ProjectRoleInProject.MEMBER;

    @NotNull(message = "Allocated effort percent is required")
    @Min(value = 0, message = "Allocated effort must be at least 0")
    @Max(value = 100, message = "Allocated effort must be at most 100")
    private Integer allocatedEffortPercent;

    private LocalDate joinDate;

    private String note;
}
