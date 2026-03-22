package com.fptu.math_master.service;

import com.fptu.math_master.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    Page<Notification> getNotifications(UUID userId, Pageable pageable);

    void markAsRead(UUID notificationId, UUID userId);

    void markAllAsRead(UUID userId);

    long getUnreadCount(UUID userId);
}
