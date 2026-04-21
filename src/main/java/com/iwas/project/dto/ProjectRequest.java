package com.iwas.project.dto;

import com.iwas.project.enums.ProjectPriority;
import com.iwas.project.enums.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(max = 200, message = "Name must be at most 200 characters")
    private String name;

    @Size(max = 50, message = "Code must be at most 50 characters")
    private String code;

    private String description;

    private ProjectStatus status = ProjectStatus.PLANNING;

    private ProjectPriority priority = ProjectPriority.MEDIUM;

    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull(message = "Manager ID is required")
    private Long managerId;
}
