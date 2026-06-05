package com.iwas.arrangement.model;

import com.iwas.task.enums.TaskPriority;

/**
 * A task as seen by the ATC engine — already reduced to the three scheduling
 * parameters and stripped of all business context.
 *
 * @param id              task identifier (used for deterministic tie-breaking)
 * @param priority        maps to the weight {@code wⱼ}
 * @param processingHours {@code pⱼ} — outstanding work in hours; may be 0 when
 *                        the estimate is missing (the engine floors it)
 * @param dueHours        {@code dⱼ} — work-hours of runway from {@code t0} to the
 *                        deadline; {@code null} means no deadline (treated as +∞)
 */
public record AtcTask(Long id, TaskPriority priority, double processingHours, Double dueHours) {
}
