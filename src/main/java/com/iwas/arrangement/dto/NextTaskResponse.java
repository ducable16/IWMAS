package com.iwas.arrangement.dto;

import com.iwas.task.enums.TaskPriority;

/**
 * Online dispatch answer: the single task the ATC rule suggests this member
 * picks up next. {@code queueEmpty} is true (and the task fields null) when the
 * lane has no eligible work.
 */
public record NextTaskResponse(Long projectId, Long assigneeId, boolean queueEmpty,
                               Long taskId, String title, TaskPriority priority,
                               double priorityIndex, String reason) {

    public static NextTaskResponse empty(Long projectId, Long assigneeId) {
        return new NextTaskResponse(projectId, assigneeId, true, null, null, null, 0.0, null);
    }
}
