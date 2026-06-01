package com.iwas.project.dto;

import com.iwas.project.enums.ProjectStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(max = 200, message = "Name must be at most 200 characters")
    private String name;

    @Pattern(
            regexp = "^[A-Za-z0-9-]{2,10}$",
            message = "Code must be 2–10 characters: letters, digits, or hyphens"
    )
    private String code;

    private String description;

    private ProjectStatus status = ProjectStatus.PLANNING;

    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull(message = "Manager ID is required")
    private Long managerId;

    // Required on create (validated in ProjectService.createProject), ignored on update.
    @Min(value = 1, message = "Manager allocated effort must be at least 1")
    @Max(value = 100, message = "Manager allocated effort must be at most 100")
    private Integer managerAllocationPercent;
}
