package com.openqualityhub.service.impl;

import com.openqualityhub.dto.request.CreateQaServiceRequest;
import com.openqualityhub.dto.response.QaServiceResponse;
import com.openqualityhub.entity.Organization;
import com.openqualityhub.entity.QaService;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.mapper.QaServiceMapper;
import com.openqualityhub.repository.OrganizationRepository;
import com.openqualityhub.repository.QaServiceRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QaServiceCatalogServiceImplTest {

    @Mock private QaServiceRepository qaServiceRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private QaServiceMapper qaServiceMapper;

    @InjectMocks
    private QaServiceCatalogServiceImpl service;

    private static final UUID DEFAULT_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private Organization org;
    private UUID serviceId;

    @BeforeEach
    void setUp() {
        org = Organization.builder().name("Demo").build();
        org.setId(DEFAULT_ORG_ID);
        serviceId = UUID.randomUUID();
    }

    @Test
    void create_shouldSaveQaService() {
        CreateQaServiceRequest req = new CreateQaServiceRequest();
        req.setName("Performance Testing");
        req.setDescription("Load and stress testing");
        req.setCategory("PERFORMANCE_TESTING");
        req.setEstimatedHours(new BigDecimal("16.0"));

        QaService entity = QaService.builder()
                .name("Performance Testing")
                .category(QaService.ServiceCategory.PERFORMANCE_TESTING)
                .build();
        entity.setId(serviceId);

        QaServiceResponse response = new QaServiceResponse();
        response.setName("Performance Testing");

        when(organizationRepository.findById(DEFAULT_ORG_ID)).thenReturn(Optional.of(org));
        when(qaServiceRepository.save(any(QaService.class))).thenReturn(entity);
        when(qaServiceMapper.toResponse(entity)).thenReturn(response);

        QaServiceResponse result = service.create(req);

        assertEquals("Performance Testing", result.getName());
        verify(qaServiceRepository).save(any(QaService.class));
        verify(organizationRepository).findById(DEFAULT_ORG_ID);
    }

    @Test
    void create_shouldSetOrganizationAndCategoryFromRequest() {
        CreateQaServiceRequest req = new CreateQaServiceRequest();
        req.setName("Security Testing");
        req.setCategory("SECURITY_TESTING");
        req.setEstimatedHours(new BigDecimal("8.0"));

        QaService saved = QaService.builder().name("Security Testing").build();
        saved.setId(serviceId);

        when(organizationRepository.findById(DEFAULT_ORG_ID)).thenReturn(Optional.of(org));
        when(qaServiceRepository.save(any(QaService.class))).thenAnswer(inv -> {
            QaService arg = inv.getArgument(0);
            assertEquals(org, arg.getOrganization());
            assertEquals(QaService.ServiceCategory.SECURITY_TESTING, arg.getCategory());
            assertTrue(arg.isActive());
            return saved;
        });
        when(qaServiceMapper.toResponse(any())).thenReturn(new QaServiceResponse());

        service.create(req);

        verify(qaServiceRepository).save(any(QaService.class));
    }

    @Test
    void create_shouldThrowWhenOrganizationNotFound() {
        CreateQaServiceRequest req = new CreateQaServiceRequest();
        req.setName("Test");
        req.setCategory("CONSULTATION");

        when(organizationRepository.findById(DEFAULT_ORG_ID)).thenReturn(Optional.empty());

        assertThrows(java.util.NoSuchElementException.class, () -> service.create(req));
    }

    @Test
    void getById_shouldUseSoftDeleteAwareQuery() {
        QaService entity = QaService.builder().name("Existing").build();
        entity.setId(serviceId);
        QaServiceResponse response = new QaServiceResponse();

        when(qaServiceRepository.findByIdAndDeletedFalse(serviceId)).thenReturn(Optional.of(entity));
        when(qaServiceMapper.toResponse(entity)).thenReturn(response);

        service.getById(serviceId);

        verify(qaServiceRepository).findByIdAndDeletedFalse(serviceId);
        verify(qaServiceRepository, never()).findById(any());
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(qaServiceRepository.findByIdAndDeletedFalse(serviceId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(serviceId));
    }

    @Test
    void update_shouldUpdateFields() {
        QaService existing = QaService.builder()
                .name("Old Name")
                .description("Old desc")
                .category(QaService.ServiceCategory.CONSULTATION)
                .build();
        existing.setId(serviceId);

        CreateQaServiceRequest req = new CreateQaServiceRequest();
        req.setName("New Name");
        req.setDescription("New desc");
        req.setCategory("TEST_AUTOMATION");
        req.setEstimatedHours(new BigDecimal("20.0"));

        QaServiceResponse response = new QaServiceResponse();
        response.setName("New Name");

        when(qaServiceRepository.findByIdAndDeletedFalse(serviceId)).thenReturn(Optional.of(existing));
        when(qaServiceRepository.save(any(QaService.class))).thenReturn(existing);
        when(qaServiceMapper.toResponse(any())).thenReturn(response);

        QaServiceResponse result = service.update(serviceId, req);

        assertEquals("New Name", result.getName());
        assertEquals("New Name", existing.getName());
        assertEquals("New desc", existing.getDescription());
        assertEquals(QaService.ServiceCategory.TEST_AUTOMATION, existing.getCategory());
        assertEquals(new BigDecimal("20.0"), existing.getEstimatedHours());
        verify(qaServiceRepository).save(existing);
    }

    @Test
    void update_shouldSkipNullFields() {
        QaService existing = QaService.builder()
                .name("Keep Name")
                .description("Keep desc")
                .category(QaService.ServiceCategory.CONSULTATION)
                .estimatedHours(new BigDecimal("10.0"))
                .build();
        existing.setId(serviceId);

        CreateQaServiceRequest req = new CreateQaServiceRequest();
        // All fields null

        when(qaServiceRepository.findByIdAndDeletedFalse(serviceId)).thenReturn(Optional.of(existing));
        when(qaServiceRepository.save(any(QaService.class))).thenReturn(existing);
        when(qaServiceMapper.toResponse(any())).thenReturn(new QaServiceResponse());

        service.update(serviceId, req);

        assertEquals("Keep Name", existing.getName());
        assertEquals("Keep desc", existing.getDescription());
        assertEquals(QaService.ServiceCategory.CONSULTATION, existing.getCategory());
        assertEquals(new BigDecimal("10.0"), existing.getEstimatedHours());
    }

    @Test
    void update_shouldThrowWhenNotFound() {
        CreateQaServiceRequest req = new CreateQaServiceRequest();
        req.setName("Updated");

        when(qaServiceRepository.findByIdAndDeletedFalse(serviceId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(serviceId, req));
    }

    @Test
    void delete_shouldSoftDelete() {
        QaService entity = QaService.builder().name("ToDelete").build();
        entity.setId(serviceId);

        when(qaServiceRepository.findByIdAndDeletedFalse(serviceId)).thenReturn(Optional.of(entity));
        when(qaServiceRepository.save(any())).thenReturn(entity);

        service.delete(serviceId);

        assertTrue(entity.isDeleted());
        verify(qaServiceRepository).save(entity);
    }

    @Test
    void delete_shouldThrowWhenNotFound() {
        when(qaServiceRepository.findByIdAndDeletedFalse(serviceId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(serviceId));
    }

    @Test
    void getAll_shouldUseSoftDeleteAwarePagination() {
        Pageable pageable = PageRequest.of(0, 10);
        QaService entity = QaService.builder().name("Svc").build();
        Page<QaService> page = new PageImpl<>(List.of(entity));
        QaServiceResponse response = new QaServiceResponse();

        when(qaServiceRepository.findByDeletedFalse(pageable)).thenReturn(page);
        when(qaServiceMapper.toResponse(entity)).thenReturn(response);

        Page<QaServiceResponse> result = service.getAll(pageable);

        assertEquals(1, result.getTotalElements());
        verify(qaServiceRepository).findByDeletedFalse(pageable);
        verify(qaServiceRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getByCategory_shouldFilterByCategoryAndActiveAndNotDeleted() {
        Pageable pageable = PageRequest.of(0, 10);
        QaService entity = QaService.builder()
                .name("Perf Test")
                .category(QaService.ServiceCategory.PERFORMANCE_TESTING)
                .build();
        Page<QaService> page = new PageImpl<>(List.of(entity));
        QaServiceResponse response = new QaServiceResponse();

        when(qaServiceRepository.findByCategoryAndDeletedFalseAndActiveTrue(
                QaService.ServiceCategory.PERFORMANCE_TESTING, pageable)).thenReturn(page);
        when(qaServiceMapper.toResponse(entity)).thenReturn(response);

        Page<QaServiceResponse> result = service.getByCategory("PERFORMANCE_TESTING", pageable);

        assertEquals(1, result.getTotalElements());
        verify(qaServiceRepository).findByCategoryAndDeletedFalseAndActiveTrue(
                QaService.ServiceCategory.PERFORMANCE_TESTING, pageable);
    }
}
