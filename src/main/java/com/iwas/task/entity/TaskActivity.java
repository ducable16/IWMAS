package com.iwas.task.entity;

import com.iwas.task.enums.TaskActivityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * A single entry in a task's unified activity/history feed. Replaces the
 * status-only {@code task_status_history}: every tracked change (status,
 * priority, assignee, estimate, dates, type, title, description) and event
 * (created, deleted, attachment added/removed) is one row here.
 *
 * <p>Values are stored raw as text — enum name, user id, number, or ISO date —
 * and resolved to display form at read time (e.g. assignee id → user).
 */
@Getter
@Setter
@Entity
@Table(
        name = "task_activity",
        indexes = {
                @Index(name = "idx_tact_task", columnList = "task_id"),
                @Index(name = "idx_tact_created_at", columnList = "created_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class TaskActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40)
    private TaskActivityType action;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
