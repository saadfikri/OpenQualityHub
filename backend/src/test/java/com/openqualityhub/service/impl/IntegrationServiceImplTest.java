package com.openqualityhub.service.impl;

import com.openqualityhub.dto.request.CreateIntegrationRequest;
import com.openqualityhub.dto.response.IntegrationConfigResponse;
import com.openqualityhub.entity.IntegrationConfig;
import com.openqualityhub.entity.Organization;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.mapper.IntegrationMapper;
import com.openqualityhub.repository.IntegrationConfigRepository;
import com.openqualityhub.repository.OrganizationRepository;
import com.openqualityhub.repository.ProjectRepository;
import com.openqualityhub.repository.WebhookEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntegrationServiceImplTest {

    @Mock private IntegrationConfigRepository integrationRepository;
    @Mock private WebhookEventRepository eventRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private IntegrationMapper mapper;

    @InjectMocks
    private IntegrationServiceImpl integrationService;

    private static final UUID DEFAULT_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private Organization org;
    private UUID integrationId;

    @BeforeEach
    void setUp() {
        org = Organization.builder().name("Demo").build();
        org.setId(DEFAULT_ORG_ID);
        integrationId = UUID.randomUUID();
    }

    @Test
    void create_shouldSaveIntegration() {
        CreateIntegrationRequest req = new CreateIntegrationRequest();
        req.setName("GitHub CI");
        req.setProvider("GITHUB");

        IntegrationConfig config = IntegrationConfig.builder()
            .name("GitHub CI").provider(IntegrationConfig.Provider.GITHUB).build();
        config.setId(integrationId);

        when(integrationRepository.existsByNameAndOrganizationIdAndDeletedFalse("GitHub CI", DEFAULT_ORG_ID))
            .thenReturn(false);
        when(organizationRepository.findById(DEFAULT_ORG_ID)).thenReturn(Optional.of(org));
        when(integrationRepository.save(any())).thenReturn(config);
        when(mapper.toResponse(any())).thenReturn(IntegrationConfigResponse.builder().name("GitHub CI").build());

        IntegrationConfigResponse result = integrationService.create(req);

        assertEquals("GitHub CI", result.getName());
        verify(integrationRepository).save(any());
    }

    @Test
    void create_shouldThrowOnDuplicateName() {
        CreateIntegrationRequest req = new CreateIntegrationRequest();
        req.setName("Existing");
        req.setProvider("GITHUB");

        when(integrationRepository.existsByNameAndOrganizationIdAndDeletedFalse("Existing", DEFAULT_ORG_ID))
            .thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> integrationService.create(req));
    }

    @Test
    void getById_shouldUseSoftDeleteAwareQuery() {
        IntegrationConfig config = IntegrationConfig.builder().name("X").provider(IntegrationConfig.Provider.GITHUB).build();
        config.setId(integrationId);

        when(integrationRepository.findByIdAndDeletedFalse(integrationId)).thenReturn(Optional.of(config));
        when(mapper.toResponse(config)).thenReturn(IntegrationConfigResponse.builder().build());

        integrationService.getById(integrationId);

        verify(integrationRepository).findByIdAndDeletedFalse(integrationId);
        verify(integrationRepository, never()).findById(any());
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(integrationRepository.findByIdAndDeletedFalse(integrationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> integrationService.getById(integrationId));
    }

    @Test
    void delete_shouldSoftDelete() {
        IntegrationConfig config = IntegrationConfig.builder()
            .name("ToDelete").provider(IntegrationConfig.Provider.GENERIC).build();
        config.setId(integrationId);

        when(integrationRepository.findByIdAndDeletedFalse(integrationId)).thenReturn(Optional.of(config));
        when(integrationRepository.save(any())).thenReturn(config);

        integrationService.delete(integrationId);

        assertTrue(config.isDeleted());
        verify(integrationRepository).save(config);
    }
}
