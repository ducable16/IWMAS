package com.roamtrip.project.dto;

import com.roamtrip.project.enums.ProjectPriority;
import com.roamtrip.project.enums.ProjectStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ProjectResponse {
    private Long id;
    private String name;
    private String code;
    private String description;
    private ProjectStatus status;
    private ProjectPriority priority;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate actualEndDate;
    private Long managerId;
    private LocalDateTime createdAt;
}
