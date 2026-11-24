package com.openqualityhub.service.impl;

import com.openqualityhub.dto.request.CreateSlaConfigurationRequest;
import com.openqualityhub.dto.response.SlaConfigurationResponse;
import com.openqualityhub.entity.Organization;
import com.openqualityhub.entity.SlaConfiguration;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.mapper.SlaConfigurationMapper;
import com.openqualityhub.repository.OrganizationRepository;
import com.openqualityhub.repository.SlaConfigurationRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlaServiceImplTest {

    @Mock private SlaConfigurationRepository slaConfigurationRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private SlaConfigurationMapper slaConfigurationMapper;

    @InjectMocks
    private SlaServiceImpl slaService;

    private static final UUID DEFAULT_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private Organization org;
    private UUID slaId;

    @BeforeEach
    void setUp() {
        org = Organization.builder().name("Demo").build();
        org.setId(DEFAULT_ORG_ID);
        slaId = UUID.randomUUID();
    }

    @Test
    void create_shouldSaveSlaConfiguration() {
        CreateSlaConfigurationRequest req = new CreateSlaConfigurationRequest();
        req.setName("P1 Critical");
        req.setDescription("Critical SLA");
        req.setPriorityLevel(1);
        req.setResponseTimeHours(1);
        req.setResolutionTimeHours(4);
        req.setBusinessHoursOnly(false);

        SlaConfiguration entity = SlaConfiguration.builder()
                .name("P1 Critical")
                .priorityLevel(1)
                .responseTimeHours(1)
                .resolutionTimeHours(4)
                .build();
        entity.setId(slaId);

        SlaConfigurationResponse response = new SlaConfigurationResponse();
        response.setName("P1 Critical");

        when(organizationRepository.findById(DEFAULT_ORG_ID)).thenReturn(Optional.of(org));
        when(slaConfigurationRepository.save(any(SlaConfiguration.class))).thenReturn(entity);
        when(slaConfigurationMapper.toResponse(entity)).thenReturn(response);

        SlaConfigurationResponse result = slaService.create(req);

        assertEquals("P1 Critical", result.getName());
        verify(slaConfigurationRepository).save(any(SlaConfiguration.class));
    }

    @Test
    void create_shouldSetOrganizationAndActiveTrue() {
        CreateSlaConfigurationRequest req = new CreateSlaConfigurationRequest();
        req.setName("P2 High");
        req.setPriorityLevel(2);
        req.setResponseTimeHours(2);
        req.setResolutionTimeHours(8);
        req.setBusinessHoursOnly(true);

        SlaConfiguration saved = SlaConfiguration.builder().name("P2 High").build();
        saved.setId(slaId);

        when(organizationRepository.findById(DEFAULT_ORG_ID)).thenReturn(Optional.of(org));
        when(slaConfigurationRepository.save(any(SlaConfiguration.class))).thenAnswer(inv -> {
            SlaConfiguration arg = inv.getArgument(0);
            assertEquals(org, arg.getOrganization());
            assertTrue(arg.isActive());
            assertEquals(2, arg.getPriorityLevel());
            return saved;
        });
        when(slaConfigurationMapper.toResponse(any())).thenReturn(new SlaConfigurationResponse());

        slaService.create(req);

        verify(slaConfigurationRepository).save(any(SlaConfiguration.class));
    }

    @Test
    void getById_shouldUseSoftDeleteAwareQuery() {
        SlaConfiguration entity = SlaConfiguration.builder().name("SLA").build();
        entity.setId(slaId);
        SlaConfigurationResponse response = new SlaConfigurationResponse();

        when(slaConfigurationRepository.findByIdAndDeletedFalse(slaId)).thenReturn(Optional.of(entity));
        when(slaConfigurationMapper.toResponse(entity)).thenReturn(response);

        slaService.getById(slaId);

        verify(slaConfigurationRepository).findByIdAndDeletedFalse(slaId);
        verify(slaConfigurationRepository, never()).findById(any());
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(slaConfigurationRepository.findByIdAndDeletedFalse(slaId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> slaService.getById(slaId));
    }

    @Test
    void update_shouldUpdateFields() {
        SlaConfiguration existing = SlaConfiguration.builder()
                .name("Old SLA")
                .description("Old desc")
                .priorityLevel(3)
                .responseTimeHours(4)
                .resolutionTimeHours(24)
                .businessHoursOnly(true)
                .build();
        existing.setId(slaId);

        CreateSlaConfigurationRequest req = new CreateSlaConfigurationRequest();
        req.setName("Updated SLA");
        req.setDescription("Updated desc");
        req.setPriorityLevel(2);
        req.setResponseTimeHours(2);
        req.setResolutionTimeHours(8);
        req.setBusinessHoursOnly(false);

        SlaConfigurationResponse response = new SlaConfigurationResponse();
        response.setName("Updated SLA");

        when(slaConfigurationRepository.findByIdAndDeletedFalse(slaId)).thenReturn(Optional.of(existing));
        when(slaConfigurationRepository.save(any())).thenReturn(existing);
        when(slaConfigurationMapper.toResponse(any())).thenReturn(response);

        SlaConfigurationResponse result = slaService.update(slaId, req);

        assertEquals("Updated SLA", result.getName());
        assertEquals("Updated SLA", existing.getName());
        assertEquals("Updated desc", existing.getDescription());
        assertEquals(2, existing.getPriorityLevel());
        assertEquals(2, existing.getResponseTimeHours());
        assertEquals(8, existing.getResolutionTimeHours());
        assertFalse(existing.isBusinessHoursOnly());
        verify(slaConfigurationRepository).save(existing);
    }

    @Test
    void update_shouldThrowWhenNotFound() {
        CreateSlaConfigurationRequest req = new CreateSlaConfigurationRequest();
        req.setName("Updated");

        when(slaConfigurationRepository.findByIdAndDeletedFalse(slaId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> slaService.update(slaId, req));
    }

    @Test
    void delete_shouldSoftDelete() {
        SlaConfiguration entity = SlaConfiguration.builder().name("ToDelete").build();
        entity.setId(slaId);

        when(slaConfigurationRepository.findByIdAndDeletedFalse(slaId)).thenReturn(Optional.of(entity));
        when(slaConfigurationRepository.save(any())).thenReturn(entity);

        slaService.delete(slaId);

        assertTrue(entity.isDeleted());
        verify(slaConfigurationRepository).save(entity);
    }

    @Test
    void delete_shouldThrowWhenNotFound() {
        when(slaConfigurationRepository.findByIdAndDeletedFalse(slaId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> slaService.delete(slaId));
    }

    @Test
    void getAll_shouldUseSoftDeleteAwarePagination() {
        Pageable pageable = PageRequest.of(0, 10);
        SlaConfiguration entity = SlaConfiguration.builder().name("SLA").build();
        Page<SlaConfiguration> page = new PageImpl<>(List.of(entity));
        SlaConfigurationResponse response = new SlaConfigurationResponse();

        when(slaConfigurationRepository.findByDeletedFalse(pageable)).thenReturn(page);
        when(slaConfigurationMapper.toResponse(entity)).thenReturn(response);

        Page<SlaConfigurationResponse> result = slaService.getAll(pageable);

        assertEquals(1, result.getTotalElements());
        verify(slaConfigurationRepository).findByDeletedFalse(pageable);
        verify(slaConfigurationRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getActive_shouldReturnActiveSlasSortedByPriority() {
        SlaConfiguration sla1 = SlaConfiguration.builder().name("P1").priorityLevel(1).build();
        SlaConfiguration sla2 = SlaConfiguration.builder().name("P2").priorityLevel(2).build();

        SlaConfigurationResponse resp1 = new SlaConfigurationResponse();
        resp1.setName("P1");
        SlaConfigurationResponse resp2 = new SlaConfigurationResponse();
        resp2.setName("P2");

        when(slaConfigurationRepository.findByDeletedFalseAndActiveTrueOrderByPriorityLevelAsc())
                .thenReturn(List.of(sla1, sla2));
        when(slaConfigurationMapper.toResponse(sla1)).thenReturn(resp1);
        when(slaConfigurationMapper.toResponse(sla2)).thenReturn(resp2);

        List<SlaConfigurationResponse> result = slaService.getActive();

        assertEquals(2, result.size());
        assertEquals("P1", result.get(0).getName());
        assertEquals("P2", result.get(1).getName());
        verify(slaConfigurationRepository).findByDeletedFalseAndActiveTrueOrderByPriorityLevelAsc();
    }
}
