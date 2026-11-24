package com.openqualityhub.service.impl;

import com.openqualityhub.dto.response.NotificationResponse;
import com.openqualityhub.entity.Notification;
import com.openqualityhub.entity.User;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.repository.NotificationRepository;
import com.openqualityhub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .email("test@demo.oqh")
                .firstName("Test")
                .lastName("User")
                .displayName("Test User")
                .passwordHash("hash")
                .build();
        user.setId(userId);
    }

    // --- send ---

    @Test
    void send_createsNotificationAndSaves() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Notification saved = Notification.builder()
                .user(user)
                .title("Title")
                .message("Message")
                .type(Notification.NotificationType.INFO)
                .link("/test")
                .build();
        saved.setId(UUID.randomUUID());
        saved.setCreatedAt(Instant.now());

        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        notificationService.send(userId, "Title", "Message", Notification.NotificationType.INFO, "/test");

        verify(userRepository).findById(userId);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void send_userNotFound_throwsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.send(userId, "T", "M", Notification.NotificationType.ALERT, null));
    }

    @Test
    void send_withRegisteredEmitter_pushesEvent() throws Exception {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Notification saved = Notification.builder()
                .user(user)
                .title("Title")
                .message("Msg")
                .type(Notification.NotificationType.SUCCESS)
                .link("/link")
                .build();
        saved.setId(UUID.randomUUID());
        saved.setCreatedAt(Instant.now());
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        SseEmitter emitter = mock(SseEmitter.class);
        notificationService.registerEmitter(userId, emitter);

        notificationService.send(userId, "Title", "Msg", Notification.NotificationType.SUCCESS, "/link");

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void send_emitterThrowsIOException_removesEmitter() throws Exception {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Notification saved = Notification.builder()
                .user(user)
                .title("T")
                .message("M")
                .type(Notification.NotificationType.WARNING)
                .build();
        saved.setId(UUID.randomUUID());
        saved.setCreatedAt(Instant.now());
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new java.io.IOException("connection closed"))
                .when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        notificationService.registerEmitter(userId, emitter);

        // Should not throw despite IOException
        assertDoesNotThrow(() ->
                notificationService.send(userId, "T", "M", Notification.NotificationType.WARNING, null));

        // Sending again should not attempt emitter.send (emitter removed)
        Notification saved2 = Notification.builder()
                .user(user)
                .title("T2")
                .message("M2")
                .type(Notification.NotificationType.INFO)
                .build();
        saved2.setId(UUID.randomUUID());
        saved2.setCreatedAt(Instant.now());
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved2);

        notificationService.send(userId, "T2", "M2", Notification.NotificationType.INFO, null);

        // emitter.send was only called once (the first time, before removal)
        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    // --- getForUser ---

    @Test
    void getForUser_returnsPageOfResponses() {
        Pageable pageable = PageRequest.of(0, 10);
        Notification n = Notification.builder()
                .user(user)
                .title("Test")
                .message("Hello")
                .type(Notification.NotificationType.INFO)
                .link("/x")
                .build();
        n.setId(UUID.randomUUID());
        n.setCreatedAt(Instant.now());

        Page<Notification> page = new PageImpl<>(List.of(n), pageable, 1);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(page);

        Page<NotificationResponse> result = notificationService.getForUser(userId, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Test", result.getContent().get(0).getTitle());
        assertEquals("INFO", result.getContent().get(0).getType());
    }

    // --- getUnreadCount ---

    @Test
    void getUnreadCount_delegatesToRepository() {
        when(notificationRepository.countByUserIdAndReadFalse(userId)).thenReturn(5L);

        long count = notificationService.getUnreadCount(userId);

        assertEquals(5L, count);
    }

    // --- markAsRead ---

    @Test
    void markAsRead_setsReadTrue() {
        UUID notifId = UUID.randomUUID();
        Notification n = Notification.builder()
                .user(user)
                .title("T")
                .message("M")
                .type(Notification.NotificationType.INFO)
                .build();
        n.setId(notifId);
        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(n));

        notificationService.markAsRead(notifId, userId);

        assertTrue(n.isRead());
        verify(notificationRepository).save(n);
    }

    @Test
    void markAsRead_notFound_throwsException() {
        UUID notifId = UUID.randomUUID();
        when(notificationRepository.findById(notifId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.markAsRead(notifId, userId));
    }

    @Test
    void markAsRead_wrongUser_throwsException() {
        UUID notifId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        User otherUser = User.builder().email("other@demo.oqh").firstName("O").lastName("U").passwordHash("h").build();
        otherUser.setId(otherUserId);

        Notification n = Notification.builder()
                .user(otherUser)
                .title("T")
                .message("M")
                .type(Notification.NotificationType.INFO)
                .build();
        n.setId(notifId);
        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(n));

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.markAsRead(notifId, userId));
    }

    // --- markAllRead ---

    @Test
    void markAllRead_delegatesToRepository() {
        notificationService.markAllRead(userId);

        verify(notificationRepository).markAllReadByUserId(userId);
    }

    // --- registerEmitter / removeEmitter ---

    @Test
    void registerEmitter_storesEmitter() {
        SseEmitter emitter = new SseEmitter();
        notificationService.registerEmitter(userId, emitter);
        // Verified indirectly - no exception
    }

    @Test
    void removeEmitter_removesEmitter() {
        SseEmitter emitter = new SseEmitter();
        notificationService.registerEmitter(userId, emitter);
        notificationService.removeEmitter(userId);
        // Verified indirectly - no exception
    }
}
