package com.roamtrip.department.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DepartmentResponse {
    private Long id;
    private String name;
    private String description;
    private Long managerId;
    private LocalDateTime createdAt;
}
