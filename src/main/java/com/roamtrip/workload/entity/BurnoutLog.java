package com.roamtrip.workload.entity;

import com.roamtrip.workload.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "burnout_logs",
        indexes = {
                @Index(name = "idx_burnout_user", columnList = "user_id"),
                @Index(name = "idx_burnout_evaluated_at", columnList = "evaluated_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class BurnoutLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(name = "overdue_task_count")
    private Integer overdueTaskCount = 0;

    @Column(name = "capacity_used_avg", precision = 5, scale = 2)
    private BigDecimal capacityUsedAvg;

    @Column(name = "is_alert_sent")
    private Boolean isAlertSent = false;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
