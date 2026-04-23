package com.iwas.task.enums;

import lombok.Getter;

@Getter
public enum TaskStatus {
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    IN_REVIEW("In Review"),
    DONE("Done"),
    CANCELLED("Cancelled");

    private final String displayName;

    TaskStatus(String displayName) {
        this.displayName = displayName;
    }

    public boolean canTransitionTo(TaskStatus next) {
        return switch (this) {
            case TODO       -> next == IN_PROGRESS || next == CANCELLED;
            case IN_PROGRESS-> next == IN_REVIEW || next == TODO || next == CANCELLED;
            case IN_REVIEW  -> next == DONE || next == IN_PROGRESS || next == TODO || next == CANCELLED;
            case DONE       -> next == IN_PROGRESS || next == TODO;
            case CANCELLED  -> next == TODO;
        };
    }
}
