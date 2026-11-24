package com.openqualityhub.service;

import com.openqualityhub.dto.request.CreateIntegrationRequest;
import com.openqualityhub.dto.request.UpdateIntegrationRequest;
import com.openqualityhub.dto.response.IntegrationConfigResponse;
import com.openqualityhub.dto.response.IntegrationDashboardResponse;
import com.openqualityhub.dto.response.WebhookEventResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IntegrationService {

    IntegrationConfigResponse create(CreateIntegrationRequest request);

    IntegrationConfigResponse update(UUID id, UpdateIntegrationRequest request);

    IntegrationConfigResponse getById(UUID id);

    Page<IntegrationConfigResponse> getAll(Pageable pageable);

    Page<IntegrationConfigResponse> getByProvider(String provider, Pageable pageable);

    void delete(UUID id);

    Page<WebhookEventResponse> getEvents(UUID integrationId, Pageable pageable);

    IntegrationDashboardResponse getDashboard();

    String getWebhookUrl(UUID integrationId);
}
