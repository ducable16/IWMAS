package com.iwas.notification.realtime;

import com.iwas.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRealtimePublisher {

    private static final String DESTINATION = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    public void pushToUser(Long recipientId, NotificationResponse payload) {
        if (recipientId == null || payload == null) {
            return;
        }
        try {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(recipientId),
                    DESTINATION,
                    payload
            );
        } catch (Exception ex) {
            log.warn("[Realtime] push failed recipientId={} notificationId={}: {}",
                    recipientId, payload.getId(), ex.getMessage());
        }
    }
}
