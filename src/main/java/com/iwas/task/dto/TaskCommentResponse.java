package com.iwas.task.dto;

import com.iwas.user.dto.UserPublicView;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class TaskCommentResponse {
    private Long id;
    private Long taskId;
    private UserPublicView author;
    private String content;
    private Map<Long, UserPublicView> mentions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
