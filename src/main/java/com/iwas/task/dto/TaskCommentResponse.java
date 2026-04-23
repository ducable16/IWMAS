package com.iwas.task.dto;

import com.iwas.user.dto.UserMeResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskCommentResponse {
    private Long id;
    private Long taskId;
    private UserMeResponse author;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
