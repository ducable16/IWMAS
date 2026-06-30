package com.iwas.arrangement.dto;

import com.iwas.task.enums.TaskPriority;

public record NextTaskResponse(Long projectId, Long assigneeId, boolean queueEmpty,
                               Long taskId, String title, TaskPriority priority) {

    public static NextTaskResponse empty(Long projectId, Long assigneeId) {
        return new NextTaskResponse(projectId, assigneeId, true, null, null, null);
    }
}
