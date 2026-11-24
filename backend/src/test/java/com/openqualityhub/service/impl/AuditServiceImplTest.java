package com.openqualityhub.service.impl;

import com.openqualityhub.dto.response.AuditEventResponse;
import com.openqualityhub.entity.AuditEvent;
import com.openqualityhub.entity.Organization;
import com.openqualityhub.entity.User;
import com.openqualityhub.repository.AuditEventRepository;
import com.openqualityhub.repository.OrganizationRepository;
import com.openqualityhub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    private static final UUID DEFAULT_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock private AuditEventRepository auditEventRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private AuditServiceImpl auditService;

    private Organization organization;

    @BeforeEach
    void setUp() {
        organization = Organization.builder().name("Test Org").build();
        organization.setId(DEFAULT_ORG_ID);
    }

    // --- log ---

    @Test
    void log_savesAuditEvent() {
        when(organizationRepository.findById(DEFAULT_ORG_ID)).thenReturn(Optional.of(organization));

        UUID entityId = UUID.randomUUID();
        auditService.log("TestRun", entityId, AuditEvent.AuditAction.CREATE, "{\"key\":\"value\"}");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());

        AuditEvent event = captor.getValue();
        assertEquals("TestRun", event.getEntityType());
        assertEquals(entityId, event.getEntityId());
        assertEquals(AuditEvent.AuditAction.CREATE, event.getAction());
        assertEquals("{\"key\":\"value\"}", event.getDetails());
        assertEquals(organization, event.getOrganization());
    }

    @Test
    void log_nullDetails_defaultsToEmptyJson() {
        when(organizationRepository.findById(DEFAULT_ORG_ID)).thenReturn(Optional.of(organization));

        auditService.log("User", UUID.randomUUID(), AuditEvent.AuditAction.UPDATE, null);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        assertEquals("{}", captor.getValue().getDetails());
    }

    @Test
    void log_organizationNotFound_skipsAuditLog() {
        when(organizationRepository.findById(DEFAULT_ORG_ID)).thenReturn(Optional.empty());

        auditService.log("User", UUID.randomUUID(), AuditEvent.AuditAction.DELETE, null);

        verify(auditEventRepository, never()).save(any());
    }

    @Test
    void log_exceptionDuringSave_doesNotPropagate() {
        when(organizationRepository.findById(DEFAULT_ORG_ID)).thenReturn(Optional.of(organization));
        when(auditEventRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        // Should not throw
        assertDoesNotThrow(() ->
                auditService.log("Defect", UUID.randomUUID(), AuditEvent.AuditAction.CREATE, "{}"));
    }

    // --- getEvents ---

    @Test
    void getEvents_noFilters_returnsAllEvents() {
        Pageable pageable = PageRequest.of(0, 10);
        User eventUser = User.builder().email("u@test.com").firstName("A").lastName("B").displayName("AB").passwordHash("h").build();
        eventUser.setId(UUID.randomUUID());

        AuditEvent event = AuditEvent.builder()
                .organization(organization)
                .user(eventUser)
                .entityType("TestRun")
                .entityId(UUID.randomUUID())
                .action(AuditEvent.AuditAction.CREATE)
                .details("{}")
                .ipAddress("127.0.0.1")
                .build();
        event.setId(UUID.randomUUID());
        event.setCreatedAt(Instant.now());

        Page<AuditEvent> page = new PageImpl<>(List.of(event), pageable, 1);
        when(auditEventRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<AuditEventResponse> result = auditService.getEvents(null, null, null, pageable);

        assertEquals(1, result.getTotalElements());
        AuditEventResponse resp = result.getContent().get(0);
        assertEquals("TestRun", resp.getEntityType());
        assertEquals("CREATE", resp.getAction());
        assertEquals("AB", resp.getUserName());
        assertEquals("127.0.0.1", resp.getIpAddress());
    }

    @Test
    void getEvents_withEntityTypeFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditEvent> page = new PageImpl<>(List.of(), pageable, 0);
        when(auditEventRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<AuditEventResponse> result = auditService.getEvents("Defect", null, null, pageable);

        assertEquals(0, result.getTotalElements());
        verify(auditEventRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getEvents_withActionFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditEvent> page = new PageImpl<>(List.of(), pageable, 0);
        when(auditEventRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<AuditEventResponse> result = auditService.getEvents(null, AuditEvent.AuditAction.DELETE, null, pageable);

        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getEvents_withUserIdFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        UUID userId = UUID.randomUUID();
        Page<AuditEvent> page = new PageImpl<>(List.of(), pageable, 0);
        when(auditEventRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<AuditEventResponse> result = auditService.getEvents(null, null, userId, pageable);

        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getEvents_withAllFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        UUID userId = UUID.randomUUID();
        Page<AuditEvent> page = new PageImpl<>(List.of(), pageable, 0);
        when(auditEventRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<AuditEventResponse> result = auditService.getEvents("User", AuditEvent.AuditAction.UPDATE, userId, pageable);

        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getEvents_nullUser_userNameIsNull() {
        Pageable pageable = PageRequest.of(0, 10);

        AuditEvent event = AuditEvent.builder()
                .organization(organization)
                .user(null)
                .entityType("TestRun")
                .entityId(UUID.randomUUID())
                .action(AuditEvent.AuditAction.IMPORT)
                .details("{}")
                .build();
        event.setId(UUID.randomUUID());
        event.setCreatedAt(Instant.now());

        Page<AuditEvent> page = new PageImpl<>(List.of(event), pageable, 1);
        when(auditEventRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<AuditEventResponse> result = auditService.getEvents(null, null, null, pageable);

        assertNull(result.getContent().get(0).getUserName());
    }

    @Test
    void getEvents_blankEntityType_noFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditEvent> page = new PageImpl<>(List.of(), pageable, 0);
        when(auditEventRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        // Blank entityType should be ignored (same as null)
        auditService.getEvents("  ", null, null, pageable);

        verify(auditEventRepository).findAll(any(Specification.class), eq(pageable));
    }
}
