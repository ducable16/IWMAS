package com.iwas.task.entity;

import com.iwas.task.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "task_status_history",
        indexes = {
                @Index(name = "idx_tsh_task", columnList = "task_id"),
                @Index(name = "idx_tsh_changed_at", columnList = "changed_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class TaskStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 50)
    private TaskStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 50)
    private TaskStatus newStatus;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @CreatedDate
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;
}
