package com.iwas.timelog.entity;

import com.iwas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(
        name = "time_logs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_time_log", columnNames = {"task_id", "user_id", "log_date"})
        },
        indexes = {
                @Index(name = "idx_timelog_task", columnList = "task_id"),
                @Index(name = "idx_timelog_user", columnList = "user_id"),
                @Index(name = "idx_timelog_date", columnList = "log_date")
        }
)
public class TimeLog extends BaseEntity {

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "hours_spent", nullable = false, precision = 4, scale = 1)
    private BigDecimal hoursSpent;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
