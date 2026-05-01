package com.iwas.project.dto;

import com.iwas.project.enums.ProjectPriority;
import com.iwas.project.enums.ProjectStatus;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ProjectFilterRequest {

    private String search;

    private List<ProjectStatus> statuses;

    private List<ProjectPriority> priorities;

    private Long managerId;

    private LocalDate startDateFrom;

    private LocalDate startDateTo;

    private LocalDate endDateFrom;

    private LocalDate endDateTo;

    private String sortBy = "createdAt";

    private String sortDirection = "DESC";

    private int page = 0;

    private int size = 20;
}
