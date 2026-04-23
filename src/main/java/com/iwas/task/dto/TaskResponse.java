package com.iwas.task.dto;

import com.iwas.task.enums.TaskPriority;
import com.iwas.task.enums.TaskStatus;
import com.iwas.task.enums.TaskType;
import com.iwas.user.dto.UserMeResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
public class TaskResponse {
    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private TaskType type;
    private TaskStatus status;
    private TaskPriority priority;
    private BigDecimal estimatedHours;
    private BigDecimal actualHours;
    private LocalDate startDate;
    private LocalDate dueDate;
    private LocalDateTime completedAt;
    private String sprint;
    private Set<String> labels;
    private UserMeResponse assignee;
    private UserMeResponse reporter;
    private List<TaskSkillRequirementResponse> skillRequirements;
    private Map<String, String> customFields;
    private List<TaskCommentResponse> comments;  // only populated on task detail
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
