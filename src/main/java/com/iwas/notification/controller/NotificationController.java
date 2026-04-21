package com.iwas.notification.controller;

import com.iwas.notification.dto.NotificationResponse;
import com.iwas.notification.service.NotificationService;
import com.iwas.security.AuthenticatedUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getAll() {
        return ResponseEntity.ok(notificationService.getMyNotifications(authenticatedUserResolver.currentUserId()));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnread() {
        return ResponseEntity.ok(notificationService.getMyUnread(authenticatedUserResolver.currentUserId()));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> countUnread() {
        return ResponseEntity.ok(Map.of("count", notificationService.countUnread(authenticatedUserResolver.currentUserId())));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(authenticatedUserResolver.currentUserId(), id));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead(authenticatedUserResolver.currentUserId());
        return ResponseEntity.noContent().build();
    }
}
