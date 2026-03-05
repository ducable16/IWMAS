package com.roamtrip.entity.ai;

import com.roamtrip.entity.base.BaseEntity;
import com.roamtrip.entity.enums.AiSessionStatus;
import com.roamtrip.entity.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

