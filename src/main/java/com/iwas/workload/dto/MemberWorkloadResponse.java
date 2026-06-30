package com.iwas.workload.dto;

import com.iwas.task.enums.TaskPriority;
import com.iwas.task.enums.TaskStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class MemberWorkloadResponse {
    private Long userId;
    private String userFullName;
    private String email;

    private BigDecimal worstBacklogDays;

    private Integer atRiskCount;
    private Integer overdueTaskCount;
    private Integer predictedLateTaskCount;

    private Integer activeTaskCount;
    private Integer unestimatedTaskCount;
    private List<TaskWorkloadItem> unestimatedTasks;

    private List<ProjectAllocationItem> projectAllocations;
    private List<TaskWorkloadItem> tasks;

    @Getter
    @Builder
    public static class TaskWorkloadItem {
        private Long taskId;
        private Long projectId;
        private String title;
        private TaskStatus status;
        private TaskPriority priority;
        private LocalDate startDate;
        private LocalDate dueDate;
        private BigDecimal remainingHours;
        private Integer executionSeq;
        private LocalDate projectedStartDate;
        private LocalDate projectedFinishDate;
        private boolean willSlip;
        private long lateByWorkdays;
        private boolean overdue;
        private boolean unestimated;
    }

    @Getter
    @Builder
    public static class ProjectAllocationItem {
        private Long projectId;
        private String projectName;
        private Integer allocatedEffortPercent;
        private BigDecimal dailyCapacityHours;
        private BigDecimal backlogHours;
        private BigDecimal backlogDays;
        private Integer overdueCount;
        private Integer predictedLateTaskCount;
    }
}
