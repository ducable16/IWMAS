package com.iwas.task.dto;

import com.iwas.task.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private TaskStatus status;
}
