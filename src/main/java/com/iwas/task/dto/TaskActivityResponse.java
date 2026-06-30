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

    private String oldValue;
    private String newValue;

    private UserPublicView oldUser;
    private UserPublicView newUser;

    private UserPublicView actor;

    private LocalDateTime createdAt;
}
