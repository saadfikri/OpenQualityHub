package com.openqualityhub.service;

import com.openqualityhub.dto.response.NotificationResponse;
import com.openqualityhub.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

public interface NotificationService {

    void send(UUID userId, String title, String message, Notification.NotificationType type, String link);

    Page<NotificationResponse> getForUser(UUID userId, Pageable pageable);

    long getUnreadCount(UUID userId);

    void markAsRead(UUID notificationId, UUID userId);

    void markAllRead(UUID userId);

    void registerEmitter(UUID userId, SseEmitter emitter);

    void removeEmitter(UUID userId);
}
