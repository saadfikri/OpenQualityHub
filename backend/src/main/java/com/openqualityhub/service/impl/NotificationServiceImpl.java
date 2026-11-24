package com.openqualityhub.service.impl;

import com.openqualityhub.dto.response.NotificationResponse;
import com.openqualityhub.entity.Notification;
import com.openqualityhub.entity.User;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.repository.NotificationRepository;
import com.openqualityhub.repository.UserRepository;
import com.openqualityhub.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private final ConcurrentHashMap<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void send(UUID userId, String title, String message, Notification.NotificationType type, String link) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .link(link)
                .build();

        notification = notificationRepository.save(notification);

        NotificationResponse response = toResponse(notification);
        pushToEmitter(userId, response);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getForUser(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification", notificationId);
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllReadByUserId(userId);
    }

    @Override
    public void registerEmitter(UUID userId, SseEmitter emitter) {
        emitters.put(userId, emitter);
        log.debug("SSE emitter registered for user {}", userId);
    }

    @Override
    public void removeEmitter(UUID userId) {
        emitters.remove(userId);
        log.debug("SSE emitter removed for user {}", userId);
    }

    private void pushToEmitter(UUID userId, NotificationResponse response) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(response));
            } catch (IOException e) {
                log.warn("Failed to send SSE event to user {}, removing emitter", userId);
                emitters.remove(userId);
            }
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType().name())
                .link(notification.getLink())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
