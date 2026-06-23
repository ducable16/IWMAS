package com.iwas.workload.dto;

import com.iwas.workload.dto.MemberWorkloadResponse.TaskWorkloadItem;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Simulation result for one member's project lane under a given task order.
 *
 * <p>Same two-axis model as the dashboard: {@code loadLevel} + {@code backlogDays}
 * are the volume (order-independent), while {@code predictedLateTaskCount} and the
 * per-task {@code willSlip} flags are the deadline-risk signals that change as the
 * member reorders.
 *
 * <p>{@code savedOrder} = true when the response reflects the member's persisted
 * executionSeq; false for an ATC suggestion or an unsaved preview.
 */
@Getter
@Builder
public class ProjectScheduleResponse {
    private Long projectId;
    private String projectName;
    private Integer allocatedEffortPercent;
    private BigDecimal dailyCapacityHours;
    /** Σ remaining of this lane's workable tasks (hours). */
    private BigDecimal backlogHours;
    /** backlogHours ÷ dailyCapacityHours = workdays to clear; null when the lane has no capacity. */
    private BigDecimal backlogDays;
    /** Tasks already past their due date in this lane. */
    private Integer overdueCount;
    /** Tasks predicted to slip but not yet overdue under this order. */
    private Integer predictedLateTaskCount;
    private boolean savedOrder;
    /** Tasks in simulated execution order; non-schedulable tasks appended last. */
    private List<TaskWorkloadItem> tasks;
}
