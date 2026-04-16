package com.roamtrip.task.dto;

import com.roamtrip.task.enums.TaskPriority;
import com.roamtrip.task.enums.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class TaskRequest {

    @NotNull(message = "Project ID is required")
    private Long projectId;

    @NotBlank(message = "Task title is required")
    @Size(max = 300, message = "Title must be at most 300 characters")
    private String title;

    private String description;

    private TaskType type = TaskType.FEATURE;

    private TaskPriority priority = TaskPriority.MEDIUM;

    private BigDecimal estimatedHours;

    private LocalDate startDate;

    private LocalDate dueDate;

    private Long assigneeId;

    private List<TaskSkillRequirementRequest> skillRequirements;
}
