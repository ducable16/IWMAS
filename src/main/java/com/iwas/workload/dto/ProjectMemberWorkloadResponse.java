package com.iwas.workload.dto;

import com.iwas.workload.dto.MemberWorkloadResponse.ProjectAllocationItem;
import com.iwas.workload.dto.MemberWorkloadResponse.TaskWorkloadItem;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Project-scoped member workload — all metrics are computed for a single project lane,
 * not aggregated across the member's full portfolio.
 */
@Getter
@Builder
public class ProjectMemberWorkloadResponse {
    private Long userId;
    private String userFullName;
    private String email;

    /** Allocation and load metrics for this project's lane. */
    private ProjectAllocationItem projectAllocation;

    private Integer activeTaskCount;
    private Integer unestimatedTaskCount;
    private List<TaskWorkloadItem> unestimatedTasks;
    private List<TaskWorkloadItem> tasks;
}