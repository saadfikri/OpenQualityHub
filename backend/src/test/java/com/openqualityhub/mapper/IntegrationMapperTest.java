package com.openqualityhub.mapper;

import com.openqualityhub.dto.response.IntegrationConfigResponse;
import com.openqualityhub.dto.response.WebhookEventResponse;
import com.openqualityhub.entity.IntegrationConfig;
import com.openqualityhub.entity.Project;
import com.openqualityhub.entity.WebhookEvent;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationMapperTest {

    private final IntegrationMapper mapper = Mappers.getMapper(IntegrationMapper.class);

    @Test
    void toResponse_shouldMapAllFields() {
        Project project = Project.builder().name("API").build();
        project.setId(UUID.randomUUID());

        IntegrationConfig config = IntegrationConfig.builder()
            .name("GitHub CI")
            .description("CI pipeline")
            .provider(IntegrationConfig.Provider.GITHUB)
            .enabled(true)
            .webhookSecret("secret123")
            .eventCount(42L)
            .lastEventAt(Instant.now())
            .project(project)
            .build();
        config.setId(UUID.randomUUID());

        IntegrationConfigResponse response = mapper.toResponse(config);

        assertEquals(config.getId(), response.getId());
        assertEquals("GitHub CI", response.getName());
        assertEquals("GITHUB", response.getProvider());
        assertTrue(response.isEnabled());
        assertTrue(response.isHasSecret());
        assertEquals(42L, response.getEventCount());
        assertEquals(project.getId(), response.getProjectId());
        assertEquals("API", response.getProjectName());
    }

    @Test
    void toResponse_shouldHandleNoSecretAndNoProject() {
        IntegrationConfig config = IntegrationConfig.builder()
            .name("Jenkins")
            .provider(IntegrationConfig.Provider.JENKINS)
            .enabled(false)
            .eventCount(0L)
            .build();
        config.setId(UUID.randomUUID());

        IntegrationConfigResponse response = mapper.toResponse(config);

        assertFalse(response.isHasSecret());
        assertFalse(response.isEnabled());
        assertNull(response.getProjectId());
        assertNull(response.getProjectName());
    }

    @Test
    void toEventResponse_shouldMapFields() {
        IntegrationConfig config = IntegrationConfig.builder()
            .provider(IntegrationConfig.Provider.GITHUB).build();
        config.setId(UUID.randomUUID());

        WebhookEvent event = WebhookEvent.builder()
            .integration(config)
            .eventType("push")
            .provider(IntegrationConfig.Provider.GITHUB)
            .status(WebhookEvent.EventStatus.PROCESSED)
            .processedAt(Instant.now())
            .build();
        event.setId(UUID.randomUUID());

        WebhookEventResponse response = mapper.toEventResponse(event);

        assertEquals(event.getId(), response.getId());
        assertEquals(config.getId(), response.getIntegrationId());
        assertEquals("push", response.getEventType());
        assertEquals("GITHUB", response.getProvider());
        assertEquals("PROCESSED", response.getStatus());
    }
}
