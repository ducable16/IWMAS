package com.roamtrip.notification.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.roamtrip.common.entity.BaseEntity;
import com.roamtrip.notification.enums.NotificationChannel;
import com.roamtrip.notification.enums.NotificationStatus;
import com.roamtrip.notification.enums.NotificationType;
import com.roamtrip.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notification_recipient_created_at", columnList = "recipient_id,created_at"),
                @Index(name = "idx_notification_recipient_status_created_at", columnList = "recipient_id,status,created_at")
        }
)
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 32)
    private NotificationChannel channel = NotificationChannel.IN_APP;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private NotificationStatus status = NotificationStatus.UNREAD;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private JsonNode payload;
}
