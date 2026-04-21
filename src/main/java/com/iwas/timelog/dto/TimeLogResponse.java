package com.iwas.timelog.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class TimeLogResponse {
    private Long id;
    private Long taskId;
    private String taskTitle;
    private Long userId;
    private LocalDate logDate;
    private BigDecimal hoursSpent;
    private String description;
    private LocalDateTime createdAt;
}
