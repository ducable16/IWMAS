package com.iwas.workload.dto;

import com.iwas.workload.dto.MemberWorkloadResponse.TaskWorkloadItem;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class ProjectScheduleResponse {
    private Long projectId;
    private String projectName;
    private Integer allocatedEffortPercent;
    private BigDecimal dailyCapacityHours;
    private BigDecimal backlogHours;
    private BigDecimal backlogDays;
    private Integer overdueCount;
    private Integer predictedLateTaskCount;
    private boolean savedOrder;
    private List<TaskWorkloadItem> tasks;
}
