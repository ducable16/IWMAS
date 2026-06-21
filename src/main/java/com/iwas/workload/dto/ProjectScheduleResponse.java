package com.iwas.workload.dto;

import com.iwas.workload.dto.MemberWorkloadResponse.TaskWorkloadItem;
import com.iwas.workload.enums.WorkloadLevel;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Simulation result for one member's project lane under a given task order.
 *
 * {@code savedOrder} = true when the response reflects the member's persisted
 * executionSeq; false for an EDD suggestion or an unsaved preview.
 */
@Getter
@Builder
public class ProjectScheduleResponse {
    private Long projectId;
    private String projectName;
    private Integer allocatedEffortPercent;
    private BigDecimal dailyCapacityHours;
    private WorkloadLevel workloadLevel;
    private BigDecimal workloadPercent;
    private Integer predictedLateTaskCount;
    private boolean savedOrder;
    /** Tasks in simulated execution order; non-schedulable tasks appended last. */
    private List<TaskWorkloadItem> tasks;
}
