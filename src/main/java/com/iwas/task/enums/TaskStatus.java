package com.iwas.task.enums;

import lombok.Getter;

@Getter
public enum TaskStatus {
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    DONE("Done"),
    CANCELLED("Cancelled");

    private final String displayName;

    TaskStatus(String displayName) {
        this.displayName = displayName;
    }

    public boolean canTransitionTo(TaskStatus next) {
        return switch (this) {
            case TODO       -> next == IN_PROGRESS || next == CANCELLED;
            case IN_PROGRESS-> next == DONE || next == TODO || next == CANCELLED;
            case DONE       -> next == IN_PROGRESS || next == TODO;
            case CANCELLED  -> next == TODO;
        };
    }
}
