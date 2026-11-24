package com.openqualityhub.mapper;

import com.openqualityhub.dto.response.SlaConfigurationResponse;
import com.openqualityhub.entity.SlaConfiguration;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SlaConfigurationMapperTest {

    private final SlaConfigurationMapper mapper = Mappers.getMapper(SlaConfigurationMapper.class);

    @Test
    void toResponse_shouldMapAllFields() {
        SlaConfiguration sla = SlaConfiguration.builder()
            .name("Critical SLA")
            .description("SLA for critical priority requests")
            .priorityLevel(1)
            .responseTimeHours(2)
            .resolutionTimeHours(24)
            .businessHoursOnly(false)
            .active(true)
            .build();
        sla.setId(UUID.randomUUID());
        sla.setCreatedAt(Instant.parse("2024-05-01T08:00:00Z"));
        sla.setUpdatedAt(Instant.parse("2024-05-10T12:00:00Z"));

        SlaConfigurationResponse response = mapper.toResponse(sla);

        assertEquals(sla.getId().toString(), response.getId());
        assertEquals("Critical SLA", response.getName());
        assertEquals("SLA for critical priority requests", response.getDescription());
        assertEquals(1, response.getPriorityLevel());
        assertEquals(2, response.getResponseTimeHours());
        assertEquals(24, response.getResolutionTimeHours());
        assertFalse(response.isBusinessHoursOnly());
        assertTrue(response.isActive());
        assertEquals("2024-05-01T08:00:00Z", response.getCreatedAt());
        assertEquals("2024-05-10T12:00:00Z", response.getUpdatedAt());
    }

    @Test
    void toResponse_shouldHandleNullTimestamps() {
        SlaConfiguration sla = SlaConfiguration.builder()
            .name("Standard SLA")
            .priorityLevel(3)
            .responseTimeHours(8)
            .resolutionTimeHours(72)
            .businessHoursOnly(true)
            .active(true)
            .build();
        sla.setId(UUID.randomUUID());

        SlaConfigurationResponse response = mapper.toResponse(sla);

        assertNull(response.getCreatedAt());
        assertNull(response.getUpdatedAt());
    }

    @Test
    void toResponse_shouldHandleNullId() {
        SlaConfiguration sla = SlaConfiguration.builder()
            .name("Low SLA")
            .priorityLevel(5)
            .responseTimeHours(24)
            .resolutionTimeHours(168)
            .build();

        SlaConfigurationResponse response = mapper.toResponse(sla);

        assertNull(response.getId());
    }

    @Test
    void toResponse_shouldReturnNullForNullInput() {
        assertNull(mapper.toResponse(null));
    }
}
