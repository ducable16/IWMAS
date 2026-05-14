package com.iwas.task.dto;

import com.iwas.user.dto.UserMeResponse;
import com.iwas.user.dto.UserPublicView;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskCommentResponse {
    private Long id;
    private Long taskId;
    private UserPublicView author;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
