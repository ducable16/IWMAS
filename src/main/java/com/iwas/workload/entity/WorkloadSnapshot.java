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

/**
 * Daily snapshot of a member's workload under the v3 schedule-simulation model.
 */
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

    @Column(name = "project_count")
    private Integer projectCount = 0;

    @Column(name = "active_task_count")
    private Integer activeTaskCount = 0;

    @Column(name = "overdue_task_count")
    private Integer overdueTaskCount = 0;

    @Column(name = "predicted_late_task_count")
    private Integer predictedLateTaskCount = 0;

    @Column(name = "unestimated_task_count")
    private Integer unestimatedTaskCount = 0;

    /** Worst near-term tightness across the member's project lanes (percent). */
    @Column(name = "near_term_percent", precision = 7, scale = 2)
    private BigDecimal nearTermPercent;

    /** Worst overall-feasibility tightness across the member's project lanes (percent). */
    @Column(name = "overall_percent", precision = 7, scale = 2)
    private BigDecimal overallPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "workload_level", length = 20)
    private WorkloadLevel workloadLevel;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
