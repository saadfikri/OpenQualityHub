package com.openqualityhub.service.impl;

import com.openqualityhub.dto.request.CreateQaServiceRequest;
import com.openqualityhub.dto.response.QaServiceResponse;
import com.openqualityhub.entity.Organization;
import com.openqualityhub.entity.QaService;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.mapper.QaServiceMapper;
import com.openqualityhub.repository.OrganizationRepository;
import com.openqualityhub.repository.QaServiceRepository;
import com.openqualityhub.service.QaServiceCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QaServiceCatalogServiceImpl implements QaServiceCatalogService {

    private static final UUID DEFAULT_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final QaServiceRepository qaServiceRepository;
    private final OrganizationRepository organizationRepository;
    private final QaServiceMapper qaServiceMapper;

    @Override
    @Transactional
    public QaServiceResponse create(CreateQaServiceRequest request) {
        Organization org = organizationRepository.findById(DEFAULT_ORG_ID).orElseThrow();

        QaService service = QaService.builder()
            .organization(org)
            .name(request.getName())
            .description(request.getDescription())
            .category(QaService.ServiceCategory.valueOf(request.getCategory()))
            .estimatedHours(request.getEstimatedHours())
            .active(true)
            .build();

        service = qaServiceRepository.save(service);
        log.info("Created QA service: {} ({})", service.getName(), service.getId());
        return qaServiceMapper.toResponse(service);
    }

    @Override
    @Transactional
    public QaServiceResponse update(UUID id, CreateQaServiceRequest request) {
        QaService service = qaServiceRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("QaService", id));

        if (request.getName() != null) service.setName(request.getName());
        if (request.getDescription() != null) service.setDescription(request.getDescription());
        if (request.getCategory() != null) service.setCategory(QaService.ServiceCategory.valueOf(request.getCategory()));
        if (request.getEstimatedHours() != null) service.setEstimatedHours(request.getEstimatedHours());

        service = qaServiceRepository.save(service);
        return qaServiceMapper.toResponse(service);
    }

    @Override
    public QaServiceResponse getById(UUID id) {
        return qaServiceMapper.toResponse(qaServiceRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("QaService", id)));
    }

    @Override
    public Page<QaServiceResponse> getAll(Pageable pageable) {
        return qaServiceRepository.findByDeletedFalse(pageable).map(qaServiceMapper::toResponse);
    }

    @Override
    public Page<QaServiceResponse> getByCategory(String category, Pageable pageable) {
        QaService.ServiceCategory cat = QaService.ServiceCategory.valueOf(category);
        return qaServiceRepository.findByCategoryAndDeletedFalseAndActiveTrue(cat, pageable)
            .map(qaServiceMapper::toResponse);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        QaService service = qaServiceRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("QaService", id));
        service.setDeleted(true);
        qaServiceRepository.save(service);
        log.info("Soft-deleted QA service: {}", id);
    }
}
