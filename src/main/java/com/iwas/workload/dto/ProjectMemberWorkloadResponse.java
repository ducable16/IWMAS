package com.iwas.workload.dto;

import com.iwas.workload.dto.MemberWorkloadResponse.ProjectAllocationItem;
import com.iwas.workload.dto.MemberWorkloadResponse.TaskWorkloadItem;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProjectMemberWorkloadResponse {
    private Long userId;
    private String userFullName;
    private String email;

    private ProjectAllocationItem projectAllocation;

    private Integer activeTaskCount;
    private Integer unestimatedTaskCount;
    private List<TaskWorkloadItem> unestimatedTasks;
    private List<TaskWorkloadItem> tasks;
}