package com.iwas.workload.dto;

import com.iwas.workload.enums.WorkloadLevel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class WorkloadSnapshotResponse {
    private Long id;
    private Long userId;
    private String userFullName;
    private LocalDate snapshotDate;
    private BigDecimal totalAllocatedHours;
    private BigDecimal totalActualHours;
    private BigDecimal capacityUsedPercent;
    private Integer projectCount;
    private Integer activeTaskCount;
    private BigDecimal weeklyCapacityHours;
    private BigDecimal weeklyLoadHours;
    private BigDecimal utilizationPercent;
    private WorkloadLevel workloadLevel;
    private Integer overdueTaskCount;
    private Integer unestimatedTaskCount;
}
