package com.iwas.project.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProjectPageResponse {
    private List<ProjectResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}