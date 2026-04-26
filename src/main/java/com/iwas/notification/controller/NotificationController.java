package com.iwas.notification.controller;

import com.iwas.notification.dto.NotificationResponse;
import com.iwas.notification.service.NotificationService;
import com.iwas.security.AuthenticatedUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public List<NotificationResponse> getAll() {
        return notificationService.getMyNotifications(authenticatedUserResolver.currentUserId());
    }

    @GetMapping("/unread")
    public List<NotificationResponse> getUnread() {
        return notificationService.getMyUnread(authenticatedUserResolver.currentUserId());
    }

    @GetMapping("/unread/count")
    public Map<String, Long> countUnread() {
        return Map.of("count", notificationService.countUnread(authenticatedUserResolver.currentUserId()));
    }

    @PostMapping("/{id}/read")
    public NotificationResponse markAsRead(@PathVariable Long id) {
        return notificationService.markAsRead(authenticatedUserResolver.currentUserId(), id);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead() {
        notificationService.markAllAsRead(authenticatedUserResolver.currentUserId());
    }
}
