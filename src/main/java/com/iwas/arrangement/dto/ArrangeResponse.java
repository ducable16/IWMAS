package com.iwas.arrangement.dto;

import com.iwas.task.enums.TaskPriority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Read-only ATC arrangement suggestion for one (project, assignee) lane.
 * Nothing is persisted — the member or PM may apply it via the existing
 * schedule-save endpoint if they choose.
 */
public record ArrangeResponse(Long projectId, Long assigneeId,
                              Integer allocatedEffortPercent, BigDecimal dailyCapacityHours,
                              double k, List<Item> tasks) {

    /**
     * One task in suggested order, enriched with calendar projections from the
     * workload simulator when the lane has capacity.
     */
    public record Item(Long taskId, String title, int position, TaskPriority priority,
                       double priorityIndex, double slackHours,
                       LocalDate projectedStart, LocalDate projectedFinish,
                       double projectedTardinessHours, long lateByWorkdays, boolean willSlip,
                       boolean estimateDefaulted, String reason) {
    }
}
