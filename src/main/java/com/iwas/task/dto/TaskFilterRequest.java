package com.iwas.task.dto;

import com.iwas.task.enums.TaskPriority;
import com.iwas.task.enums.TaskStatus;
import com.iwas.task.enums.TaskType;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class TaskFilterRequest {

    private String search;

    private Long projectId;

    private List<TaskStatus> statuses;

    private Long assigneeId;

    private Long reporterId;

    private List<TaskPriority> priorities;

    private List<TaskType> types;

    private LocalDate dueDateFrom;

    private LocalDate dueDateTo;

    private String sortBy = "createdAt";

    private String sortDirection = "DESC";

    private int page = 0;

    private int size = 20;
}
