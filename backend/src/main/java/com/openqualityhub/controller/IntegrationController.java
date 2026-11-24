package com.openqualityhub.controller;

import com.openqualityhub.dto.request.CreateIntegrationRequest;
import com.openqualityhub.dto.request.UpdateIntegrationRequest;
import com.openqualityhub.dto.response.IntegrationConfigResponse;
import com.openqualityhub.dto.response.IntegrationDashboardResponse;
import com.openqualityhub.dto.response.WebhookEventResponse;
import com.openqualityhub.service.IntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integrations")
@RequiredArgsConstructor
@Tag(name = "Integrations", description = "External integration configuration and event log")
public class IntegrationController {

    private final IntegrationService integrationService;

    @PostMapping
    @Operation(summary = "Create a new integration configuration")
    @PreAuthorize("hasAuthority('INTEGRATION_WRITE')")
    public ResponseEntity<IntegrationConfigResponse> create(
            @Valid @RequestBody CreateIntegrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(integrationService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an integration configuration")
    @PreAuthorize("hasAuthority('INTEGRATION_WRITE')")
    public ResponseEntity<IntegrationConfigResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateIntegrationRequest request) {
        return ResponseEntity.ok(integrationService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an integration configuration by ID")
    @PreAuthorize("hasAuthority('INTEGRATION_READ')")
    public ResponseEntity<IntegrationConfigResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(integrationService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List all integration configurations")
    @PreAuthorize("hasAuthority('INTEGRATION_READ')")
    public ResponseEntity<Page<IntegrationConfigResponse>> getAll(
            @RequestParam(required = false) String provider,
            @PageableDefault(size = 20) Pageable pageable) {
        if (provider != null) {
            return ResponseEntity.ok(integrationService.getByProvider(provider, pageable));
        }
        return ResponseEntity.ok(integrationService.getAll(pageable));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an integration configuration")
    @PreAuthorize("hasAuthority('INTEGRATION_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        integrationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/events")
    @Operation(summary = "Get webhook event log for an integration")
    @PreAuthorize("hasAuthority('INTEGRATION_READ')")
    public ResponseEntity<Page<WebhookEventResponse>> getEvents(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(integrationService.getEvents(id, pageable));
    }

    @GetMapping("/{id}/webhook-url")
    @Operation(summary = "Get the webhook URL for an integration")
    @PreAuthorize("hasAuthority('INTEGRATION_READ')")
    public ResponseEntity<String> getWebhookUrl(@PathVariable UUID id) {
        return ResponseEntity.ok(integrationService.getWebhookUrl(id));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get integration dashboard statistics")
    @PreAuthorize("hasAuthority('INTEGRATION_READ')")
    public ResponseEntity<IntegrationDashboardResponse> getDashboard() {
        return ResponseEntity.ok(integrationService.getDashboard());
    }
}
