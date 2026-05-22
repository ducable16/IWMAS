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
    private Integer projectCount;
    private Integer activeTaskCount;
    private Integer overdueTaskCount;
    private Integer predictedLateTaskCount;
    private Integer unestimatedTaskCount;
    private BigDecimal nearTermPercent;
    private BigDecimal overallPercent;
    private WorkloadLevel workloadLevel;
}
