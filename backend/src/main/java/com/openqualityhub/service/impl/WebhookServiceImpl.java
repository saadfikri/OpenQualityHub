package com.openqualityhub.service.impl;

import com.openqualityhub.entity.IntegrationConfig;
import com.openqualityhub.entity.Notification;
import com.openqualityhub.entity.User;
import com.openqualityhub.entity.WebhookEvent;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.repository.IntegrationConfigRepository;
import com.openqualityhub.repository.UserRepository;
import com.openqualityhub.repository.WebhookEventRepository;
import com.openqualityhub.service.NotificationService;
import com.openqualityhub.service.WebhookService;
import com.openqualityhub.service.webhook.HmacValidator;
import com.openqualityhub.service.webhook.WebhookEventProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class WebhookServiceImpl implements WebhookService {

    private final IntegrationConfigRepository integrationRepository;
    private final WebhookEventRepository eventRepository;
    private final HmacValidator hmacValidator;
    private final List<WebhookEventProcessor> processors;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public WebhookServiceImpl(IntegrationConfigRepository integrationRepository,
                              WebhookEventRepository eventRepository,
                              HmacValidator hmacValidator,
                              List<WebhookEventProcessor> processors,
                              NotificationService notificationService,
                              UserRepository userRepository) {
        this.integrationRepository = integrationRepository;
        this.eventRepository = eventRepository;
        this.hmacValidator = hmacValidator;
        this.processors = processors;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void receive(UUID integrationId, String payload, Map<String, String> headers) {
        IntegrationConfig config = integrationRepository.findByIdAndDeletedFalse(integrationId)
                .orElseThrow(() -> new ResourceNotFoundException("IntegrationConfig", integrationId));

        String eventType = detectEventType(config.getProvider(), headers);
        String headersJson = toJson(headers);

        WebhookEvent event = WebhookEvent.builder()
                .integration(config)
                .eventType(eventType)
                .provider(config.getProvider())
                .status(WebhookEvent.EventStatus.RECEIVED)
                .payload(payload != null ? payload : "{}")
                .headers(headersJson)
                .build();
        event = eventRepository.save(event);

        // Validate webhook secret — REQUIRED for security
        if (config.getWebhookSecret() == null || config.getWebhookSecret().isBlank()) {
            event.setStatus(WebhookEvent.EventStatus.FAILED);
            event.setProcessingError("Webhook secret not configured — rejecting unsigned event");
            event.setProcessedAt(Instant.now());
            eventRepository.save(event);
            log.warn("Webhook rejected for integration {} — no secret configured", integrationId);
            return;
        }

        boolean valid;
        if (config.getProvider() == IntegrationConfig.Provider.GITLAB) {
            valid = validateGitLabSecret(config.getWebhookSecret(), payload, headers);
        } else {
            String signature = extractSignature(config.getProvider(), headers);
            valid = hmacValidator.validate(payload, config.getWebhookSecret(), signature);
        }
        if (!valid) {
            event.setStatus(WebhookEvent.EventStatus.FAILED);
            event.setProcessingError("Secret/signature validation failed");
            event.setProcessedAt(Instant.now());
            eventRepository.save(event);
            log.warn("Secret validation failed for integration {} event {}", integrationId, event.getId());
            return;
        }

        // Check if integration is enabled
        if (!config.isEnabled()) {
            event.setStatus(WebhookEvent.EventStatus.IGNORED);
            event.setProcessingError("Integration is disabled");
            event.setProcessedAt(Instant.now());
            eventRepository.save(event);
            return;
        }

        // Dispatch to processor
        event.setStatus(WebhookEvent.EventStatus.PROCESSING);
        eventRepository.save(event);

        try {
            WebhookEventProcessor processor = findProcessor(config.getProvider(), eventType);
            if (processor != null) {
                processor.process(event);
                event.setStatus(WebhookEvent.EventStatus.PROCESSED);
            } else {
                event.setStatus(WebhookEvent.EventStatus.SKIPPED);
                event.setProcessingError("No processor found for event type: " + eventType);
            }
        } catch (Exception e) {
            event.setStatus(WebhookEvent.EventStatus.FAILED);
            event.setProcessingError(e.getMessage());
            log.error("Error processing webhook event {}: {}", event.getId(), e.getMessage(), e);
        }

        event.setProcessedAt(Instant.now());
        eventRepository.save(event);

        // Update integration stats
        config.setLastEventAt(Instant.now());
        config.setEventCount(config.getEventCount() + 1);
        integrationRepository.save(config);

        log.info("Webhook event {} processed: status={}", event.getId(), event.getStatus());
    }

    private String detectEventType(IntegrationConfig.Provider provider, Map<String, String> headers) {
        return switch (provider) {
            case GITHUB -> headers.getOrDefault("x-github-event", "unknown");
            case GITLAB -> headers.getOrDefault("x-gitlab-event", "unknown");
            case JENKINS -> headers.getOrDefault("x-jenkins-event", "build_completed");
            case JIRA -> headers.getOrDefault("x-atlassian-webhook-identifier", "jira:issue_updated");
            case GENERIC -> headers.getOrDefault("x-event-type", "generic");
        };
    }

    private String extractSignature(IntegrationConfig.Provider provider, Map<String, String> headers) {
        return switch (provider) {
            case GITHUB -> headers.getOrDefault("x-hub-signature-256", "");
            case GITLAB -> headers.getOrDefault("x-gitlab-token", "");
            case JENKINS -> headers.getOrDefault("x-jenkins-signature", "");
            case JIRA -> headers.getOrDefault("x-hub-signature", "");
            case GENERIC -> headers.getOrDefault("x-webhook-signature", "");
        };
    }

    /**
     * Validates GitLab webhook secret using either the modern signing token (HMAC-SHA256)
     * or the legacy X-Gitlab-Token header (plain-text comparison).
     * See: https://docs.gitlab.com/user/project/integrations/webhooks/#signing-tokens
     */
    private boolean validateGitLabSecret(String secret, String payload, Map<String, String> headers) {
        // Modern: GitLab signing token (Standard Webhooks scheme)
        String webhookSignature = headers.getOrDefault("webhook-signature", "");
        String webhookId = headers.getOrDefault("webhook-id", "");
        String webhookTimestamp = headers.getOrDefault("webhook-timestamp", "");

        if (!webhookSignature.isBlank() && !webhookId.isBlank() && !webhookTimestamp.isBlank()) {
            try {
                // Strip "whsec_" prefix if present in stored secret
                String signingKey = secret.startsWith("whsec_") ? secret.substring(6) : secret;
                byte[] keyBytes = java.util.Base64.getDecoder().decode(signingKey);

                // Signed content: "webhook-id.webhook-timestamp.body"
                String signedContent = webhookId + "." + webhookTimestamp + "." + payload;

                javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
                mac.init(new javax.crypto.spec.SecretKeySpec(keyBytes, "HmacSHA256"));
                byte[] hash = mac.doFinal(signedContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                String computed = "v1," + java.util.Base64.getEncoder().encodeToString(hash);

                // webhook-signature may contain multiple signatures separated by spaces
                for (String sig : webhookSignature.split(" ")) {
                    if (java.security.MessageDigest.isEqual(
                            computed.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                            sig.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                        return true;
                    }
                }
                log.warn("GitLab signing token validation failed — signature mismatch");
                return false;
            } catch (Exception e) {
                log.warn("GitLab signing token validation error: {}", e.getMessage());
                return false;
            }
        }

        // Legacy: X-Gitlab-Token plain-text comparison
        String token = headers.getOrDefault("x-gitlab-token", "");
        if (!token.isBlank()) {
            return java.security.MessageDigest.isEqual(
                    secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        log.warn("GitLab webhook has no authentication headers (x-gitlab-token or webhook-signature)");
        return false;
    }

    private WebhookEventProcessor findProcessor(IntegrationConfig.Provider provider, String eventType) {
        return processors.stream()
                .filter(p -> p.getProvider() == provider && p.supports(eventType))
                .findFirst()
                .orElse(null);
    }

    private String toJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":\"")
              .append(escapeJson(entry.getValue())).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                     .replace("\"", "\\\"")
                     .replace("\n", "\\n")
                     .replace("\r", "\\r")
                     .replace("\t", "\\t");
    }
}
