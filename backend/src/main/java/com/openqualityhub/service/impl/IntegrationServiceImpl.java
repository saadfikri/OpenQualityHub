package com.openqualityhub.service.impl;

import com.openqualityhub.dto.request.CreateIntegrationRequest;
import com.openqualityhub.dto.request.UpdateIntegrationRequest;
import com.openqualityhub.dto.response.IntegrationConfigResponse;
import com.openqualityhub.dto.response.IntegrationDashboardResponse;
import com.openqualityhub.dto.response.WebhookEventResponse;
import com.openqualityhub.entity.IntegrationConfig;
import com.openqualityhub.entity.WebhookEvent;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.mapper.IntegrationMapper;
import com.openqualityhub.repository.IntegrationConfigRepository;
import com.openqualityhub.repository.OrganizationRepository;
import com.openqualityhub.repository.ProjectRepository;
import com.openqualityhub.repository.WebhookEventRepository;
import com.openqualityhub.service.IntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IntegrationServiceImpl implements IntegrationService {

    private static final UUID DEFAULT_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final IntegrationConfigRepository integrationRepository;
    private final WebhookEventRepository eventRepository;
    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final IntegrationMapper mapper;

    @Override
    @Transactional
    public IntegrationConfigResponse create(CreateIntegrationRequest request) {
        IntegrationConfig.Provider provider = IntegrationConfig.Provider.valueOf(request.getProvider());

        if (integrationRepository.existsByNameAndOrganizationIdAndDeletedFalse(request.getName(), DEFAULT_ORG_ID)) {
            throw new IllegalArgumentException("Integration with name '" + request.getName() + "' already exists");
        }

        IntegrationConfig config = IntegrationConfig.builder()
                .organization(organizationRepository.findById(DEFAULT_ORG_ID).orElseThrow())
                .name(request.getName())
                .description(request.getDescription())
                .provider(provider)
                .webhookSecret(request.getWebhookSecret())
                .config(request.getConfig() != null ? request.getConfig() : "{}")
                .build();

        if (request.getProjectId() != null) {
            projectRepository.findByIdAndDeletedFalse(request.getProjectId())
                    .ifPresent(config::setProject);
        }

        config = integrationRepository.save(config);
        log.info("Created integration: {} ({}) provider={}", config.getName(), config.getId(), provider);
        return mapper.toResponse(config);
    }

    @Override
    @Transactional
    public IntegrationConfigResponse update(UUID id, UpdateIntegrationRequest request) {
        IntegrationConfig config = integrationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("IntegrationConfig", id));

        if (request.getName() != null) config.setName(request.getName());
        if (request.getDescription() != null) config.setDescription(request.getDescription());
        if (request.getWebhookSecret() != null) config.setWebhookSecret(request.getWebhookSecret());
        if (request.getEnabled() != null) config.setEnabled(request.getEnabled());
        if (request.getConfig() != null) config.setConfig(request.getConfig());
        if (request.getProjectId() != null) {
            projectRepository.findByIdAndDeletedFalse(request.getProjectId())
                    .ifPresent(config::setProject);
        }

        config = integrationRepository.save(config);
        return mapper.toResponse(config);
    }

    @Override
    public IntegrationConfigResponse getById(UUID id) {
        return mapper.toResponse(integrationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("IntegrationConfig", id)));
    }

    @Override
    public Page<IntegrationConfigResponse> getAll(Pageable pageable) {
        return integrationRepository.findByDeletedFalse(pageable).map(mapper::toResponse);
    }

    @Override
    public Page<IntegrationConfigResponse> getByProvider(String provider, Pageable pageable) {
        IntegrationConfig.Provider p = IntegrationConfig.Provider.valueOf(provider);
        return integrationRepository.findByProviderAndDeletedFalse(p, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        IntegrationConfig config = integrationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("IntegrationConfig", id));
        config.setDeleted(true);
        integrationRepository.save(config);
        log.info("Soft-deleted integration: {}", id);
    }

    @Override
    public Page<WebhookEventResponse> getEvents(UUID integrationId, Pageable pageable) {
        integrationRepository.findByIdAndDeletedFalse(integrationId)
                .orElseThrow(() -> new ResourceNotFoundException("IntegrationConfig", integrationId));
        return eventRepository.findByIntegrationIdOrderByReceivedAtDesc(integrationId, pageable)
                .map(mapper::toEventResponse);
    }

    @Override
    public IntegrationDashboardResponse getDashboard() {
        long total = integrationRepository.countByDeletedFalse();
        long active = integrationRepository.countByEnabledTrueAndDeletedFalse();

        long processed = eventRepository.countByStatus(WebhookEvent.EventStatus.PROCESSED);
        long failed = eventRepository.countByStatus(WebhookEvent.EventStatus.FAILED);

        var byProvider = Arrays.stream(IntegrationConfig.Provider.values())
                .map(p -> IntegrationDashboardResponse.ProviderCount.builder()
                        .provider(p.name())
                        .count(integrationRepository.countByProviderAndDeletedFalse(p))
                        .build())
                .filter(pc -> pc.getCount() > 0)
                .collect(Collectors.toList());

        var byStatus = Arrays.stream(WebhookEvent.EventStatus.values())
                .map(s -> IntegrationDashboardResponse.EventStatusCount.builder()
                        .status(s.name())
                        .count(eventRepository.countByStatus(s))
                        .build())
                .filter(sc -> sc.getCount() > 0)
                .collect(Collectors.toList());

        return IntegrationDashboardResponse.builder()
                .totalIntegrations(total)
                .activeIntegrations(active)
                .totalEventsProcessed(processed)
                .totalEventsFailed(failed)
                .byProvider(byProvider)
                .eventsByStatus(byStatus)
                .build();
    }

    @Override
    public String getWebhookUrl(UUID integrationId) {
        integrationRepository.findByIdAndDeletedFalse(integrationId)
                .orElseThrow(() -> new ResourceNotFoundException("IntegrationConfig", integrationId));
        return "/api/v1/webhooks/" + integrationId;
    }
}
