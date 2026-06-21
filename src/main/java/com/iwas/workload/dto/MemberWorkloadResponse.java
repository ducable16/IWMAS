package com.iwas.workload.dto;

import com.iwas.task.enums.TaskPriority;
import com.iwas.task.enums.TaskStatus;
import com.iwas.workload.enums.WorkloadLevel;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Real-time workload of a member under the v3 schedule-simulation model.
 *
 * {@code workloadPercent} is a tightness ratio (cumulative demand / available
 * capacity across all deadlines) and may exceed 100. {@code workloadLevel} is
 * the aggregate risk badge across the member's project lanes — the primary
 * signal for a PM.
 */
@Getter
@Builder
public class MemberWorkloadResponse {
    private Long userId;
    private String userFullName;
    private String position;
    /** Aggregate risk badge — the worst lane badge across all the member's projects. */
    private WorkloadLevel workloadLevel;
    /** Worst tightness across lanes (cumulative demand / capacity over all deadlines). */
    private BigDecimal workloadPercent;
    private Integer activeTaskCount;
    private Integer overdueTaskCount;
    /** Tasks the simulation predicts will miss their deadline (excludes already-overdue). */
    private Integer predictedLateTaskCount;
    /** Active tasks with no usable estimate and no reported remaining — load is unknown. */
    private Integer unestimatedTaskCount;
    /** Per-project lane breakdown — null for endpoints that don't compute it. */
    private List<ProjectAllocationItem> projectAllocations;
    /** null when called from the project-member list; populated for individual user view. */
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
        /** Outstanding effort fed to the simulation (reported remaining, else estimate). */
        private BigDecimal remainingHours;
        /** Member's planned execution order within the lane; null → EDD fallback. */
        private Integer executionSeq;
        /** Simulated day this task is first worked. */
        private LocalDate projectedStartDate;
        /** Simulated completion day; null when it cannot be scheduled. */
        private LocalDate projectedFinishDate;
        /** true when projectedFinishDate is after dueDate. */
        private boolean willSlip;
        /** Workdays the projected finish runs past the due date (0 when not slipping). */
        private long lateByWorkdays;
        private boolean overdue;
        /** true when the task has no usable estimate and no reported remaining. */
        private boolean unestimated;
    }

    @Getter
    @Builder
    public static class ProjectAllocationItem {
        private Long projectId;
        private String projectName;
        /** null when user is the project's manager without a ProjectMember row. */
        private Integer allocatedEffortPercent;
        private BigDecimal dailyCapacityHours;
        private WorkloadLevel workloadLevel;
        private BigDecimal workloadPercent;
        private Integer predictedLateTaskCount;
    }
}
