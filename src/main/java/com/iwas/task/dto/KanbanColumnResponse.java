package com.iwas.task.dto;

import com.iwas.task.enums.TaskStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class KanbanColumnResponse {
    private TaskStatus status;
    private String displayName;
    private List<TaskResponse> tasks;
    private int count;
}
