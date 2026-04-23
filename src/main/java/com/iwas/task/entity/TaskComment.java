package com.iwas.task.entity;

import com.iwas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "task_comments",
        indexes = {
                @Index(name = "idx_tc_task", columnList = "task_id"),
                @Index(name = "idx_tc_author", columnList = "author_id")
        }
)
public class TaskComment extends BaseEntity {

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
}
