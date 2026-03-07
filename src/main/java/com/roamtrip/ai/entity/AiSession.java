package com.roamtrip.ai.entity;

import com.roamtrip.common.entity.BaseEntity;
import com.roamtrip.ai.enums.AiSessionStatus;
import com.roamtrip.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "ai_sessions",
        indexes = {
                @Index(name = "idx_ai_session_user", columnList = "user_id")
        }
)
public class AiSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AiSessionStatus status = AiSessionStatus.ACTIVE;
}
