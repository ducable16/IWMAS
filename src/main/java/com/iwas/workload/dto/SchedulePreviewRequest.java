package com.iwas.workload.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * A member's proposed task order for one project lane. Used to preview the
 * scheduling consequences and, on confirm, to persist the execution order.
 */
@Data
public class SchedulePreviewRequest {

    @NotNull(message = "Project id is required")
    private Long projectId;

    /** Must be exactly the lane's schedulable task ids (positive remaining), in the desired order. */
    @NotNull(message = "Ordered task ids are required")
    private List<Long> orderedTaskIds;
}
