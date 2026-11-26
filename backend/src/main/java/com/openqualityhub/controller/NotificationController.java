package com.openqualityhub.controller;

import com.openqualityhub.dto.response.NotificationResponse;
import com.openqualityhub.security.UserPrincipal;
import com.openqualityhub.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notifications with SSE streaming")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List notifications for the current user")
    public ResponseEntity<Page<NotificationResponse>> list(
            Authentication auth,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID userId = ((UserPrincipal) auth.getPrincipal()).getId();
        return ResponseEntity.ok(notificationService.getForUser(userId, pageable));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<Long> unreadCount(Authentication auth) {
        UUID userId = ((UserPrincipal) auth.getPrincipal()).getId();
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id, Authentication auth) {
        UUID userId = ((UserPrincipal) auth.getPrincipal()).getId();
        notificationService.markAsRead(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllRead(Authentication auth) {
        UUID userId = ((UserPrincipal) auth.getPrincipal()).getId();
        notificationService.markAllRead(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "SSE stream for real-time notifications")
    public SseEmitter stream(Authentication auth) {
        UUID userId = ((UserPrincipal) auth.getPrincipal()).getId();
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        notificationService.registerEmitter(userId, emitter);
        emitter.onCompletion(() -> notificationService.removeEmitter(userId));
        emitter.onTimeout(() -> notificationService.removeEmitter(userId));
        emitter.onError(e -> notificationService.removeEmitter(userId));
        return emitter;
    }
}
