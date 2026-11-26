package com.openqualityhub.service.impl;

import com.openqualityhub.entity.IntegrationConfig;
import com.openqualityhub.entity.WebhookEvent;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.repository.IntegrationConfigRepository;
import com.openqualityhub.repository.UserRepository;
import com.openqualityhub.repository.WebhookEventRepository;
import com.openqualityhub.service.NotificationService;
import com.openqualityhub.service.webhook.HmacValidator;
import com.openqualityhub.service.webhook.WebhookEventProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceImplTest {

    @Mock private IntegrationConfigRepository integrationRepository;
    @Mock private WebhookEventRepository eventRepository;
    @Mock private HmacValidator hmacValidator;
    @Mock private WebhookEventProcessor githubProcessor;
    @Mock private NotificationService notificationService;
    @Mock private UserRepository userRepository;

    private WebhookServiceImpl webhookService;
    private IntegrationConfig config;
    private UUID integrationId;

    @BeforeEach
    void setUp() {
        webhookService = new WebhookServiceImpl(
            integrationRepository, eventRepository, hmacValidator, List.of(githubProcessor),
            notificationService, userRepository
        );

        integrationId = UUID.randomUUID();
        config = IntegrationConfig.builder()
            .provider(IntegrationConfig.Provider.GITHUB)
            .enabled(true)
            .eventCount(0L)
            .webhookSecret("test-secret")
            .build();
        config.setId(integrationId);
    }

    @Test
    void receive_shouldThrowWhenIntegrationNotFound() {
        when(integrationRepository.findByIdAndDeletedFalse(integrationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> webhookService.receive(integrationId, "{}", Map.of()));
    }

    @Test
    void receive_shouldSaveEventAndProcessSuccessfully() {
        when(integrationRepository.findByIdAndDeletedFalse(integrationId)).thenReturn(Optional.of(config));
        when(eventRepository.save(any(WebhookEvent.class))).thenAnswer(inv -> {
            WebhookEvent e = inv.getArgument(0);
            if (e.getId() == null) e.setId(UUID.randomUUID());
            return e;
        });
        when(hmacValidator.validate(any(), eq("test-secret"), any())).thenReturn(true);
        when(githubProcessor.getProvider()).thenReturn(IntegrationConfig.Provider.GITHUB);
        when(githubProcessor.supports("push")).thenReturn(true);

        Map<String, String> headers = Map.of("x-github-event", "push");
        webhookService.receive(integrationId, "{\"ref\":\"main\"}", headers);

        verify(githubProcessor).process(any(WebhookEvent.class));
        verify(integrationRepository).save(config);
        assertEquals(1L, config.getEventCount());
    }

    @Test
    void receive_shouldRejectWhenNoSecretConfigured() {
        config.setWebhookSecret(null);
        when(integrationRepository.findByIdAndDeletedFalse(integrationId)).thenReturn(Optional.of(config));
        when(eventRepository.save(any(WebhookEvent.class))).thenAnswer(inv -> {
            WebhookEvent e = inv.getArgument(0);
            if (e.getId() == null) e.setId(UUID.randomUUID());
            return e;
        });

        webhookService.receive(integrationId, "{}", Map.of("x-github-event", "push"));

        ArgumentCaptor<WebhookEvent> captor = ArgumentCaptor.forClass(WebhookEvent.class);
        verify(eventRepository, atLeast(2)).save(captor.capture());
        WebhookEvent last = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(WebhookEvent.EventStatus.FAILED, last.getStatus());
        assertTrue(last.getProcessingError().contains("secret not configured"));
        verify(githubProcessor, never()).process(any());
    }

    @Test
    void receive_shouldFailOnHmacMismatch() {
        config.setWebhookSecret("mysecret");
        when(integrationRepository.findByIdAndDeletedFalse(integrationId)).thenReturn(Optional.of(config));
        when(eventRepository.save(any(WebhookEvent.class))).thenAnswer(inv -> {
            WebhookEvent e = inv.getArgument(0);
            if (e.getId() == null) e.setId(UUID.randomUUID());
            return e;
        });
        when(hmacValidator.validate(any(), eq("mysecret"), any())).thenReturn(false);

        webhookService.receive(integrationId, "{}", Map.of("x-hub-signature-256", "sha256=bad"));

        // Event should be saved as FAILED
        ArgumentCaptor<WebhookEvent> captor = ArgumentCaptor.forClass(WebhookEvent.class);
        verify(eventRepository, atLeast(2)).save(captor.capture());
        WebhookEvent last = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(WebhookEvent.EventStatus.FAILED, last.getStatus());
        assertNotNull(last.getProcessingError());
        // Should NOT call processor or update integration stats
        verify(githubProcessor, never()).process(any());
        verify(integrationRepository, never()).save(any());
    }

    @Test
    void receive_shouldIgnoreDisabledIntegration() {
        config.setEnabled(false);
        when(integrationRepository.findByIdAndDeletedFalse(integrationId)).thenReturn(Optional.of(config));
        when(eventRepository.save(any(WebhookEvent.class))).thenAnswer(inv -> {
            WebhookEvent e = inv.getArgument(0);
            if (e.getId() == null) e.setId(UUID.randomUUID());
            return e;
        });
        when(hmacValidator.validate(any(), eq("test-secret"), any())).thenReturn(true);

        webhookService.receive(integrationId, "{}", Map.of("x-github-event", "push"));

        ArgumentCaptor<WebhookEvent> captor = ArgumentCaptor.forClass(WebhookEvent.class);
        verify(eventRepository, atLeast(2)).save(captor.capture());
        WebhookEvent last = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(WebhookEvent.EventStatus.IGNORED, last.getStatus());
        verify(githubProcessor, never()).process(any());
    }

    @Test
    void receive_shouldSkipWhenNoProcessorFound() {
        when(integrationRepository.findByIdAndDeletedFalse(integrationId)).thenReturn(Optional.of(config));
        when(eventRepository.save(any(WebhookEvent.class))).thenAnswer(inv -> {
            WebhookEvent e = inv.getArgument(0);
            if (e.getId() == null) e.setId(UUID.randomUUID());
            return e;
        });
        when(hmacValidator.validate(any(), eq("test-secret"), any())).thenReturn(true);
        when(githubProcessor.getProvider()).thenReturn(IntegrationConfig.Provider.GITHUB);
        when(githubProcessor.supports("unknown_event")).thenReturn(false);

        webhookService.receive(integrationId, "{}", Map.of("x-github-event", "unknown_event"));

        ArgumentCaptor<WebhookEvent> captor = ArgumentCaptor.forClass(WebhookEvent.class);
        verify(eventRepository, atLeast(2)).save(captor.capture());
        List<WebhookEvent> saved = captor.getAllValues();
        WebhookEvent last = saved.get(saved.size() - 1);
        assertEquals(WebhookEvent.EventStatus.SKIPPED, last.getStatus());
    }

    @Test
    void receive_shouldHandleProcessorException() {
        when(integrationRepository.findByIdAndDeletedFalse(integrationId)).thenReturn(Optional.of(config));
        when(eventRepository.save(any(WebhookEvent.class))).thenAnswer(inv -> {
            WebhookEvent e = inv.getArgument(0);
            if (e.getId() == null) e.setId(UUID.randomUUID());
            return e;
        });
        when(hmacValidator.validate(any(), eq("test-secret"), any())).thenReturn(true);
        when(githubProcessor.getProvider()).thenReturn(IntegrationConfig.Provider.GITHUB);
        when(githubProcessor.supports("push")).thenReturn(true);
        doThrow(new RuntimeException("Processing error")).when(githubProcessor).process(any());

        webhookService.receive(integrationId, "{}", Map.of("x-github-event", "push"));

        ArgumentCaptor<WebhookEvent> captor = ArgumentCaptor.forClass(WebhookEvent.class);
        verify(eventRepository, atLeast(2)).save(captor.capture());
        List<WebhookEvent> saved = captor.getAllValues();
        WebhookEvent last = saved.get(saved.size() - 1);
        assertEquals(WebhookEvent.EventStatus.FAILED, last.getStatus());
        assertEquals("Processing error", last.getProcessingError());
    }
}
