package com.openqualityhub.mapper;

import com.openqualityhub.dto.response.QaServiceResponse;
import com.openqualityhub.entity.QaService;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class QaServiceMapperTest {

    private final QaServiceMapper mapper = Mappers.getMapper(QaServiceMapper.class);

    @Test
    void toResponse_shouldMapAllFields() {
        QaService service = QaService.builder()
            .name("Performance Testing")
            .description("Load and stress testing service")
            .category(QaService.ServiceCategory.PERFORMANCE_TESTING)
            .estimatedHours(new BigDecimal("40.5"))
            .active(true)
            .build();
        service.setId(UUID.randomUUID());
        service.setCreatedAt(Instant.parse("2024-06-01T10:00:00Z"));
        service.setUpdatedAt(Instant.parse("2024-06-15T14:30:00Z"));

        QaServiceResponse response = mapper.toResponse(service);

        assertEquals(service.getId().toString(), response.getId());
        assertEquals("Performance Testing", response.getName());
        assertEquals("Load and stress testing service", response.getDescription());
        assertEquals("PERFORMANCE_TESTING", response.getCategory());
        assertEquals(new BigDecimal("40.5"), response.getEstimatedHours());
        assertTrue(response.isActive());
        assertEquals("2024-06-01T10:00:00Z", response.getCreatedAt());
        assertEquals("2024-06-15T14:30:00Z", response.getUpdatedAt());
    }

    @Test
    void toResponse_shouldHandleNullCategory() {
        QaService service = QaService.builder()
            .name("Custom Service")
            .active(false)
            .build();
        service.setId(UUID.randomUUID());

        QaServiceResponse response = mapper.toResponse(service);

        assertEquals(service.getId().toString(), response.getId());
        assertEquals("Custom Service", response.getName());
        assertNull(response.getCategory());
        assertFalse(response.isActive());
    }

    @Test
    void toResponse_shouldHandleNullTimestamps() {
        QaService service = QaService.builder()
            .name("Automation")
            .category(QaService.ServiceCategory.TEST_AUTOMATION)
            .active(true)
            .build();
        service.setId(UUID.randomUUID());
        // createdAt and updatedAt are null

        QaServiceResponse response = mapper.toResponse(service);

        assertNull(response.getCreatedAt());
        assertNull(response.getUpdatedAt());
    }

    @Test
    void toResponse_shouldHandleNullId() {
        QaService service = QaService.builder()
            .name("Exploratory")
            .category(QaService.ServiceCategory.EXPLORATORY_TESTING)
            .active(true)
            .build();
        // id is null

        QaServiceResponse response = mapper.toResponse(service);

        assertNull(response.getId());
    }

    @Test
    void toResponse_shouldReturnNullForNullInput() {
        assertNull(mapper.toResponse(null));
    }
}
