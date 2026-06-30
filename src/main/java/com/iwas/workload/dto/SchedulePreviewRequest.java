package com.iwas.workload.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SchedulePreviewRequest {

    @NotNull(message = "Project id is required")
    private Long projectId;

    @NotNull(message = "Ordered task ids are required")
    private List<Long> orderedTaskIds;
}
