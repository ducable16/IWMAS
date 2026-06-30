package com.iwas.arrangement.model;

import com.iwas.task.enums.TaskPriority;

public record AtcTask(Long id, TaskPriority priority, double processingHours, Double dueHours) {
}
