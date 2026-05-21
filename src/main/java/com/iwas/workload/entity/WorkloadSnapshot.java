package com.iwas.workload.entity;

import com.iwas.workload.enums.WorkloadLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "workload_snapshots",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_workload_snapshot", columnNames = {"user_id", "snapshot_date"})
        },
        indexes = {
                @Index(name = "idx_ws_user", columnList = "user_id"),
                @Index(name = "idx_ws_date", columnList = "snapshot_date")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class WorkloadSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "total_allocated_hours", precision = 6, scale = 1)
    private BigDecimal totalAllocatedHours;

    @Column(name = "total_actual_hours", precision = 6, scale = 1)
    private BigDecimal totalActualHours;

    @Column(name = "capacity_used_percent", precision = 5, scale = 2)
    private BigDecimal capacityUsedPercent;

    @Column(name = "project_count")
    private Integer projectCount = 0;

    @Column(name = "active_task_count")
    private Integer activeTaskCount = 0;

    @Column(name = "weekly_capacity_hours", precision = 6, scale = 1)
    private BigDecimal weeklyCapacityHours;

    /** Integrated virtual-burn load over the snapshot week — NOT spare time. */
    @Column(name = "weekly_load_hours", precision = 6, scale = 1)
    private BigDecimal weeklyLoadHours;

    @Column(name = "utilization_percent", precision = 5, scale = 2)
    private BigDecimal utilizationPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "workload_level", length = 20)
    private WorkloadLevel workloadLevel;

    @Column(name = "overdue_task_count")
    private Integer overdueTaskCount = 0;

    @Column(name = "unestimated_task_count")
    private Integer unestimatedTaskCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
