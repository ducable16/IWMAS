package com.iwas.task.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class KanbanBoardResponse {
    private Long projectId;
    private List<KanbanColumnResponse> columns;
}
