package com.iwas.timelog.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TimeLogRequest {

    @NotNull(message = "Task ID is required")
    private Long taskId;

    @NotNull(message = "Log date is required")
    private LocalDate logDate;

    @NotNull(message = "Hours spent is required")
    @DecimalMin(value = "0.1", message = "Hours must be at least 0.1")
    @DecimalMax(value = "24.0", message = "Hours cannot exceed 24")
    private BigDecimal hoursSpent;

    /**
     * Optional member-reported remaining effort on the task as of logDate.
     * When present it is snapshotted onto the task and feeds the workload
     * schedule simulation. Use 0 to signal the task is finished.
     */
    @DecimalMin(value = "0.0", message = "Remaining hours cannot be negative")
    @DecimalMax(value = "9999.9", message = "Remaining hours is too large")
    private BigDecimal remainingHours;

    private String description;
}
