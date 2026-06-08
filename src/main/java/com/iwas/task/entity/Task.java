package com.iwas.task.entity;

import com.iwas.common.entity.BaseEntity;
import com.iwas.task.enums.TaskPriority;
import com.iwas.task.enums.TaskStatus;
import com.iwas.task.enums.TaskType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "tasks",
        indexes = {
                @Index(name = "idx_task_project", columnList = "project_id"),
                @Index(name = "idx_task_assignee", columnList = "assignee_id"),
                @Index(name = "idx_task_status", columnList = "status"),
                @Index(name = "idx_task_due_date", columnList = "due_date"),
                @Index(name = "idx_task_reporter", columnList = "reporter_id"),
                @Index(name = "idx_task_priority", columnList = "priority")
        }
)
public class Task extends BaseEntity {

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private TaskType type = TaskType.FEATURE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Column(name = "estimated_hours", precision = 6, scale = 1)
    private BigDecimal estimatedHours;

    @Column(name = "actual_hours", precision = 6, scale = 1)
    private BigDecimal actualHours;

    /**
     * Member-reported remaining effort, snapshotted from the most recent
     * end-of-day TimeLog. The workload v3 simulation uses this as the source
     * of truth for a task's outstanding work; falls back to estimatedHours
     * when never reported. Sticky — a stale value is kept as-is (a missed log
     * day simply means no progress was made).
     */
    @Column(name = "reported_remaining_hours", precision = 6, scale = 1)
    private BigDecimal reportedRemainingHours;

    /** logDate of the TimeLog that produced reportedRemainingHours. */
    @Column(name = "remaining_reported_date")
    private LocalDate remainingReportedDate;

    /**
     * Member's planned execution order within their (assignee, project) lane.
     * A forecasting hint only — the v3 simulation is forward-only and never
     * assumes this order was historically followed; reality is absorbed
     * through reportedRemainingHours. Null tasks fall back to EDD ordering.
     */
    @Column(name = "execution_seq")
    private Integer executionSeq;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "last_overdue_notified_at")
    private LocalDate lastOverdueNotifiedAt;
}
