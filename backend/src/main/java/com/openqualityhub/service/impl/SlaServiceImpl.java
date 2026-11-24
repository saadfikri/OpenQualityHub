package com.openqualityhub.service.impl;

import com.openqualityhub.dto.request.CreateSlaConfigurationRequest;
import com.openqualityhub.dto.response.SlaConfigurationResponse;
import com.openqualityhub.entity.Organization;
import com.openqualityhub.entity.SlaConfiguration;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.mapper.SlaConfigurationMapper;
import com.openqualityhub.repository.OrganizationRepository;
import com.openqualityhub.repository.SlaConfigurationRepository;
import com.openqualityhub.service.SlaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlaServiceImpl implements SlaService {

    private static final UUID DEFAULT_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final SlaConfigurationRepository slaConfigurationRepository;
    private final OrganizationRepository organizationRepository;
    private final SlaConfigurationMapper slaConfigurationMapper;

    @Override
    @Transactional
    public SlaConfigurationResponse create(CreateSlaConfigurationRequest request) {
        Organization org = organizationRepository.findById(DEFAULT_ORG_ID).orElseThrow();

        SlaConfiguration sla = SlaConfiguration.builder()
            .organization(org)
            .name(request.getName())
            .description(request.getDescription())
            .priorityLevel(request.getPriorityLevel())
            .responseTimeHours(request.getResponseTimeHours())
            .resolutionTimeHours(request.getResolutionTimeHours())
            .businessHoursOnly(request.isBusinessHoursOnly())
            .active(true)
            .build();

        sla = slaConfigurationRepository.save(sla);
        log.info("Created SLA configuration: {} (P{})", sla.getName(), sla.getPriorityLevel());
        return slaConfigurationMapper.toResponse(sla);
    }

    @Override
    @Transactional
    public SlaConfigurationResponse update(UUID id, CreateSlaConfigurationRequest request) {
        SlaConfiguration sla = slaConfigurationRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("SlaConfiguration", id));

        if (request.getName() != null) sla.setName(request.getName());
        if (request.getDescription() != null) sla.setDescription(request.getDescription());
        if (request.getPriorityLevel() != null) sla.setPriorityLevel(request.getPriorityLevel());
        if (request.getResponseTimeHours() != null) sla.setResponseTimeHours(request.getResponseTimeHours());
        if (request.getResolutionTimeHours() != null) sla.setResolutionTimeHours(request.getResolutionTimeHours());
        sla.setBusinessHoursOnly(request.isBusinessHoursOnly());

        sla = slaConfigurationRepository.save(sla);
        return slaConfigurationMapper.toResponse(sla);
    }

    @Override
    public SlaConfigurationResponse getById(UUID id) {
        return slaConfigurationMapper.toResponse(slaConfigurationRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("SlaConfiguration", id)));
    }

    @Override
    public Page<SlaConfigurationResponse> getAll(Pageable pageable) {
        return slaConfigurationRepository.findByDeletedFalse(pageable)
            .map(slaConfigurationMapper::toResponse);
    }

    @Override
    public List<SlaConfigurationResponse> getActive() {
        return slaConfigurationRepository.findByDeletedFalseAndActiveTrueOrderByPriorityLevelAsc()
            .stream().map(slaConfigurationMapper::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        SlaConfiguration sla = slaConfigurationRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("SlaConfiguration", id));
        sla.setDeleted(true);
        slaConfigurationRepository.save(sla);
        log.info("Soft-deleted SLA configuration: {}", id);
    }
}
