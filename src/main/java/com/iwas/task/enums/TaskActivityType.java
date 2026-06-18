package com.iwas.task.enums;

/**
 * Types of activity recorded in a task's unified history feed.
 * Field-change types carry the before/after values in
 * {@code old_value}/{@code new_value}; event types (created, deleted,
 * attachment add/remove) use only the value that applies.
 */
public enum TaskActivityType {
    TASK_CREATED,
    STATUS_CHANGED,
    PRIORITY_CHANGED,
    ASSIGNEE_CHANGED,
    ESTIMATE_CHANGED,
    TYPE_CHANGED,
    TITLE_CHANGED,
    DESCRIPTION_CHANGED,
    START_DATE_CHANGED,
    DUE_DATE_CHANGED,
    ATTACHMENT_ADDED,
    ATTACHMENT_REMOVED,
    TASK_DELETED
}
