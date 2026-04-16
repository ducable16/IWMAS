package com.roamtrip.task.entity;

import com.roamtrip.skill.enums.SkillLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "task_skill_requirements",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_task_skill", columnNames = {"task_id", "skill_id"})
        },
        indexes = {
                @Index(name = "idx_tsr_task", columnList = "task_id"),
                @Index(name = "idx_tsr_skill", columnList = "skill_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class TaskSkillRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    @Enumerated(EnumType.STRING)
    @Column(name = "minimum_level", nullable = false, length = 20)
    private SkillLevel minimumLevel = SkillLevel.INTERMEDIATE;

    @Column(name = "is_required")
    private Boolean isRequired = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;
}
