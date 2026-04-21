package com.iwas.task.dto;

import com.iwas.task.enums.TaskStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskStatusHistoryResponse {
    private Long id;
    private TaskStatus oldStatus;
    private TaskStatus newStatus;
    private Long changedBy;
    private String note;
    private LocalDateTime changedAt;
}
