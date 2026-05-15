package com.iwas.workload.dto;

import com.iwas.task.enums.TaskPriority;
import com.iwas.task.enums.TaskStatus;
import com.iwas.workload.enums.WorkloadLevel;
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
    private String position;
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private BigDecimal weeklyCapacityHours;
    private BigDecimal weeklyRemainingHours;
    private BigDecimal utilizationPercent;
    private WorkloadLevel workloadLevel;
    private Integer activeTaskCount;
    private Integer overdueTaskCount;
    /** Per-project breakdown — null for endpoints that don't compute it. */
    private List<ProjectAllocationItem> projectAllocations;
    /** null when called from the project-member list; populated for individual user view */
    private List<TaskWorkloadItem> tasks;

    @Getter
    @Builder
    public static class TaskWorkloadItem {
        private Long taskId;
        private String title;
        private TaskStatus status;
        private TaskPriority priority;
        private LocalDate dueDate;
        private BigDecimal remainingHours;
        private boolean overdue;
    }

    @Getter
    @Builder
    public static class ProjectAllocationItem {
        private Long projectId;
        private String projectName;
        /** null when user is the project's manager without a ProjectMember row. */
        private Integer allocatedEffortPercent;
        private BigDecimal dailyCapacityHours;
        private BigDecimal loadInWindowHours;
        /** null when level is BLOCKED / UNDEFINED. */
        private BigDecimal utilizationPercent;
        private WorkloadLevel workloadLevel;
    }
}
