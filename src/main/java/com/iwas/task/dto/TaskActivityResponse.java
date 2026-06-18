package com.iwas.task.dto;

import com.iwas.task.enums.TaskActivityType;
import com.iwas.user.dto.UserPublicView;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskActivityResponse {
    private Long id;
    private TaskActivityType action;

    /** Raw before/after values (enum name, number, ISO date, file name, or user id). */
    private String oldValue;
    private String newValue;

    /** Resolved users for {@code ASSIGNEE_CHANGED} — null for other actions. */
    private UserPublicView oldUser;
    private UserPublicView newUser;

    /** Who performed the action. */
    private UserPublicView actor;

    private LocalDateTime createdAt;
}
