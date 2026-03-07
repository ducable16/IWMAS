package com.roamtrip.comment.entity;

import com.roamtrip.common.entity.BaseEntity;
import com.roamtrip.issue.entity.Issue;
import com.roamtrip.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "comments",
        indexes = {
                @Index(name = "idx_comment_issue_created_at", columnList = "issue_id,created_at")
        }
)
public class Comment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Column(name = "body", nullable = false, columnDefinition = "text")
    private String body;
}
