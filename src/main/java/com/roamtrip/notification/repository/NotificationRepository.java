package com.roamtrip.notification.repository;

import com.roamtrip.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.recipientId = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByRecipientId(Long userId);

    @Query("SELECT n FROM Notification n WHERE n.recipientId = :userId AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByRecipientId(Long userId);

    long countByRecipientIdAndIsReadFalse(Long recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.recipientId = :userId AND n.isRead = false")
    void markAllAsRead(Long userId);
}
