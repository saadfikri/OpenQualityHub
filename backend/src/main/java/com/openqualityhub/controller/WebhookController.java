package com.openqualityhub.controller;

import com.openqualityhub.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Unauthenticated webhook receiver endpoints")
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/{integrationId}")
    @Operation(summary = "Receive a webhook event (unauthenticated, HMAC-validated)")
    public ResponseEntity<Void> receiveWebhook(
            @PathVariable UUID integrationId,
            @RequestBody String payload,
            HttpServletRequest request) {

        Map<String, String> headers = extractHeaders(request);
        webhookService.receive(integrationId, payload, headers);
        return ResponseEntity.accepted().build();
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement().toLowerCase();
            // Only capture relevant webhook headers
            if (name.startsWith("x-") || name.startsWith("webhook-") || name.equals("content-type") || name.equals("user-agent")) {
                headers.put(name, request.getHeader(name));
            }
        }
        return headers;
    }
}
