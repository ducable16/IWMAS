package com.iwas.task.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskCommentRequest {

    @NotBlank(message = "Content must not be blank")
    private String content;
}
