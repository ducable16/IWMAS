package com.iwas.task.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TaskPageResponse {
    private List<TaskResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
