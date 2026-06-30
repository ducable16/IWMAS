package com.iwas.project.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectManagerChangeRequest {

    @NotNull(message = "New manager ID is required")
    private Long newManagerId;

    @NotNull(message = "Manager allocated effort is required")
    @Min(value = 1, message = "Manager allocated effort must be at least 1")
    @Max(value = 100, message = "Manager allocated effort must be at most 100")
    private Integer managerAllocationPercent;
}
