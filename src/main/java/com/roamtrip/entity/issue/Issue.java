package com.roamtrip.entity.issue;

import com.roamtrip.entity.base.BaseEntity;
import com.roamtrip.entity.enums.IssuePriority;
import com.roamtrip.entity.enums.IssueType;
import com.roamtrip.entity.project.Project;
import com.roamtrip.entity.sprint.Sprint;
import com.roamtrip.entity.user.User;
import com.roamtrip.entity.workflow.WorkflowStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(
        name = "issues",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_issue_key", columnNames = {"project_id", "issue_key"})
        },
        indexes = {
                @Index(name = "idx_issue_project_status", columnList = "project_id,status_id"),
                @Index(name = "idx_issue_project_sprint", columnList = "project_id,sprint_id"),
                @Index(name = "idx_issue_project_assignee", columnList = "project_id,assignee_id"),
                @Index(name = "idx_issue_project_parent", columnList = "project_id,parent_id"),
                @Index(name = "idx_issue_project_due", columnList = "project_id,due_date")
        }
)
public class Issue extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "issue_key", nullable = false, length = 32)
    private String issueKey;

    @Column(name = "title", nullable = false, length = 512)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private IssueType type = IssueType.TASK;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 32)
    private IssuePriority priority = IssuePriority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id")
    private WorkflowStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Issue parent;

    @Column(name = "due_date")
    private LocalDate dueDate;
}

