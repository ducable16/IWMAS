package com.iwas.arrangement.dto;

import com.iwas.task.enums.TaskPriority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ArrangeResponse(Long projectId, Long assigneeId,
                              Integer allocatedEffortPercent, BigDecimal dailyCapacityHours,
                              double k, List<Item> tasks) {

    public record Item(Long taskId, String title, int position, TaskPriority priority,
                       double slackHours,
                       LocalDate projectedStart, LocalDate projectedFinish,
                       double projectedTardinessHours, long lateByWorkdays, boolean willSlip,
                       boolean estimateDefaulted, String reason) {
    }
}
