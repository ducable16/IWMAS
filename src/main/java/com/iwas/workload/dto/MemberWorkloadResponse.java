package com.iwas.workload.dto;

import com.iwas.task.enums.TaskPriority;
import com.iwas.task.enums.TaskStatus;
import com.iwas.workload.enums.LoadLevel;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Dashboard "member load" payload — two independent axes shown side by side:
 *
 * <ul>
 *   <li><b>Workload</b> — {@code loadLevel} + {@code worstBacklogDays}: how many workdays of
 *       work are queued vs daily capacity. Volume only; order- and deadline-independent.</li>
 *   <li><b>Risk</b> — {@code atRiskCount} (= {@code overdueTaskCount + predictedLateTaskCount}):
 *       tasks projected to miss (or already past) their deadline. Order-dependent (ATC).</li>
 * </ul>
 *
 * The two never replace each other: a member can be lightly loaded yet at risk
 * (a near deadline), or heavily loaded yet safe (far deadlines).
 */
@Getter
@Builder
public class MemberWorkloadResponse {
    private Long userId;
    private String userFullName;
    private String email;

    // ── Workload axis (volume) ──────────────────────────────────────────────
    /** Aggregate load badge — the worst lane (most workdays of queued work). */
    private LoadLevel loadLevel;
    /** Backlog of the most-loaded lane, in workdays of work; null when no lane has capacity. */
    private BigDecimal worstBacklogDays;

    // ── Risk axis (deadlines) ───────────────────────────────────────────────
    /** Tasks projected to miss their deadline: overdue + predicted-late, summed across lanes. */
    private Integer atRiskCount;
    private Integer overdueTaskCount;
    /** Predicted to slip but not yet overdue (subset of atRiskCount). */
    private Integer predictedLateTaskCount;

    private Integer activeTaskCount;
    /** Active tasks with no usable estimate and no reported remaining — load is undercounted. */
    private Integer unestimatedTaskCount;

    /** Per-project lane breakdown — null for endpoints that don't compute it. */
    private List<ProjectAllocationItem> projectAllocations;
    /** null when called from a list view; populated for the single-user view. */
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
        /** Member's planned execution order within the lane; null → ATC fallback. */
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
        /** Σ remaining of this lane's workable tasks (hours). */
        private BigDecimal backlogHours;
        /** backlogHours ÷ dailyCapacityHours = workdays to clear; null when the lane has no capacity. */
        private BigDecimal backlogDays;
        private LoadLevel loadLevel;
        /** Tasks already past their due date in this lane. */
        private Integer overdueCount;
        /** Tasks predicted to slip but not yet overdue in this lane. */
        private Integer predictedLateTaskCount;
    }
}
