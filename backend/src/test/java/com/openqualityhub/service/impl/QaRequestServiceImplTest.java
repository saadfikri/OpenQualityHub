package com.openqualityhub.service.impl;

import com.openqualityhub.dto.request.CreateQaServiceRequestRequest;
import com.openqualityhub.dto.response.QaServiceRequestResponse;
import com.openqualityhub.entity.*;
import com.openqualityhub.exception.InvalidStateTransitionException;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.mapper.QaServiceRequestMapper;
import com.openqualityhub.repository.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QaRequestServiceImplTest {

    @Mock private QaServiceRequestRepository requestRepository;
    @Mock private QaServiceRepository qaServiceRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private SlaConfigurationRepository slaConfigurationRepository;
    @Mock private QaServiceRequestMapper requestMapper;
    @Mock private PrioritizationEngine prioritizationEngine;

    @InjectMocks
    private QaRequestServiceImpl qaRequestService;

    private static final UUID DEFAULT_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private Organization org;
    private UUID requestId;
    private UUID userId;
    private UUID teamId;
    private UUID qaServiceId;

    @BeforeEach
    void setUp() {
        org = Organization.builder().name("Demo").build();
        org.setId(DEFAULT_ORG_ID);
        requestId = UUID.randomUUID();
        userId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        qaServiceId = UUID.randomUUID();
    }

    private CreateQaServiceRequestRequest buildCreateRequest() {
        CreateQaServiceRequestRequest req = new CreateQaServiceRequestRequest();
        req.setTitle("Test Request");
        req.setDescription("Description");
        req.setQaServiceId(qaServiceId);
        req.setRequestingTeamId(teamId);
        req.setImpact("HIGH");
        req.setUrgency("MEDIUM");
        return req;
    }

    private QaService buildQaService() {
        QaService svc = QaService.builder()
                .name("Test Service")
                .category(QaService.ServiceCategory.TEST_EXECUTION)
                .estimatedHours(new BigDecimal("8.0"))
                .build();
        svc.setId(qaServiceId);
        return svc;
    }

    private Team buildTeam() {
        Team team = Team.builder().name("QA Core").build();
        team.setId(teamId);
        return team;
    }

    private User buildUser() {
        User user = User.builder()
                .firstName("John")
                .lastName("Doe")
                .displayName("John Doe")
                .email("john@test.com")
                .passwordHash("hash")
                .build();
        user.setId(userId);
        return user;
    }

    private QaServiceRequest buildRequest(QaServiceRequest.RequestStatus status) {
        QaServiceRequest request = QaServiceRequest.builder()
                .requestNumber("QA-0001")
                .title("Test Request")
                .status(status)
                .impact(QaServiceRequest.Impact.HIGH)
                .urgency(QaServiceRequest.Urgency.MEDIUM)
                .priorityScore(2)
                .qaService(buildQaService())
                .requestingTeam(buildTeam())
                .requestingUser(buildUser())
                .organization(org)
                .activities(new ArrayList<>())
                .build();
        request.setId(requestId);
        return request;
    }

    @Test
    void create_shouldCreateRequest() {
        CreateQaServiceRequestRequest req = buildCreateRequest();
        QaService qaService = buildQaService();
        Team team = buildTeam();
        User user = buildUser();

        when(organizationRepository.findById(DEFAULT_ORG_ID)).thenReturn(Optional.of(org));
        when(qaServiceRepository.findByIdAndDeletedFalse(qaServiceId)).thenReturn(Optional.of(qaService));
        when(teamRepository.findByIdAndDeletedFalse(teamId)).thenReturn(Optional.of(team));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(prioritizationEngine.computePriority(QaServiceRequest.Impact.HIGH, QaServiceRequest.Urgency.MEDIUM))
                .thenReturn(2);
        when(requestRepository.getNextRequestNumber()).thenReturn(1L);
        when(slaConfigurationRepository.findByPriorityLevelAndOrganizationIdAndDeletedFalse(2, DEFAULT_ORG_ID))
                .thenReturn(Optional.empty());
        when(requestRepository.save(any(QaServiceRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(requestMapper.toResponse(any())).thenReturn(new QaServiceRequestResponse());

        QaServiceRequestResponse result = qaRequestService.create(req, userId);

        assertNotNull(result);
        verify(requestRepository, times(2)).save(any(QaServiceRequest.class));
    }

    @Test
    void create_shouldSetDefaultStatus() {
        CreateQaServiceRequestRequest req = buildCreateRequest();
        QaService qaService = buildQaService();
        Team team = buildTeam();
        User user = buildUser();

        when(organizationRepository.findById(DEFAULT_ORG_ID)).thenReturn(Optional.of(org));
        when(qaServiceRepository.findByIdAndDeletedFalse(qaServiceId)).thenReturn(Optional.of(qaService));
        when(teamRepository.findByIdAndDeletedFalse(teamId)).thenReturn(Optional.of(team));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(prioritizationEngine.computePriority(any(), any())).thenReturn(3);
        when(requestRepository.getNextRequestNumber()).thenReturn(2L);
        when(slaConfigurationRepository.findByPriorityLevelAndOrganizationIdAndDeletedFalse(anyInt(), any()))
                .thenReturn(Optional.empty());
        when(requestRepository.save(any(QaServiceRequest.class))).thenAnswer(inv -> {
            QaServiceRequest saved = inv.getArgument(0);
            assertEquals(QaServiceRequest.RequestStatus.SUBMITTED, saved.getStatus());
            return saved;
        });
        when(requestMapper.toResponse(any())).thenReturn(new QaServiceRequestResponse());

        qaRequestService.create(req, userId);

        verify(requestRepository, times(2)).save(argThat(r ->
                ((QaServiceRequest) r).getStatus() == QaServiceRequest.RequestStatus.SUBMITTED));
    }

    @Test
    void create_shouldThrowWhenTeamNotFound() {
        CreateQaServiceRequestRequest req = buildCreateRequest();
        QaService qaService = buildQaService();

        when(organizationRepository.findById(DEFAULT_ORG_ID)).thenReturn(Optional.of(org));
        when(qaServiceRepository.findByIdAndDeletedFalse(qaServiceId)).thenReturn(Optional.of(qaService));
        when(teamRepository.findByIdAndDeletedFalse(teamId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> qaRequestService.create(req, userId));
    }

    @Test
    void create_shouldThrowWhenServiceNotFound() {
        CreateQaServiceRequestRequest req = buildCreateRequest();

        when(organizationRepository.findById(DEFAULT_ORG_ID)).thenReturn(Optional.of(org));
        when(qaServiceRepository.findByIdAndDeletedFalse(qaServiceId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> qaRequestService.create(req, userId));
    }

    @Test
    void getById_shouldUseSoftDeleteAwareQuery() {
        QaServiceRequest request = buildRequest(QaServiceRequest.RequestStatus.SUBMITTED);
        QaServiceRequestResponse response = new QaServiceRequestResponse();

        when(requestRepository.findByIdAndDeletedFalse(requestId)).thenReturn(Optional.of(request));
        when(requestMapper.toResponse(request)).thenReturn(response);

        qaRequestService.getById(requestId);

        verify(requestRepository).findByIdAndDeletedFalse(requestId);
        verify(requestRepository, never()).findById(any());
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(requestRepository.findByIdAndDeletedFalse(requestId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> qaRequestService.getById(requestId));
    }

    @Test
    void transition_shouldUpdateStatus() {
        QaServiceRequest request = buildRequest(QaServiceRequest.RequestStatus.SUBMITTED);

        when(requestRepository.findByIdAndDeletedFalse(requestId)).thenReturn(Optional.of(request));
        when(userRepository.findById(userId)).thenReturn(Optional.of(buildUser()));
        when(requestRepository.save(any(QaServiceRequest.class))).thenReturn(request);
        when(requestMapper.toResponse(any())).thenReturn(new QaServiceRequestResponse());

        qaRequestService.transition(requestId, "TRIAGED", userId, "Triaging now");

        assertEquals(QaServiceRequest.RequestStatus.TRIAGED, request.getStatus());
        verify(requestRepository).save(request);
    }

    @Test
    void transition_shouldThrowOnInvalidTransition() {
        QaServiceRequest request = buildRequest(QaServiceRequest.RequestStatus.SUBMITTED);

        when(requestRepository.findByIdAndDeletedFalse(requestId)).thenReturn(Optional.of(request));
        when(userRepository.findById(userId)).thenReturn(Optional.of(buildUser()));

        // SUBMITTED cannot go directly to COMPLETED
        assertThrows(InvalidStateTransitionException.class,
                () -> qaRequestService.transition(requestId, "COMPLETED", userId, null));
    }

    @Test
    void transition_shouldSetRespondedAtOnTriaged() {
        QaServiceRequest request = buildRequest(QaServiceRequest.RequestStatus.SUBMITTED);
        assertNull(request.getRespondedAt());

        when(requestRepository.findByIdAndDeletedFalse(requestId)).thenReturn(Optional.of(request));
        when(userRepository.findById(userId)).thenReturn(Optional.of(buildUser()));
        when(requestRepository.save(any())).thenReturn(request);
        when(requestMapper.toResponse(any())).thenReturn(new QaServiceRequestResponse());

        qaRequestService.transition(requestId, "TRIAGED", userId, null);

        assertNotNull(request.getRespondedAt());
    }

    @Test
    void transition_shouldSetResolvedAtOnCompleted() {
        QaServiceRequest request = buildRequest(QaServiceRequest.RequestStatus.IN_PROGRESS);
        assertNull(request.getResolvedAt());

        when(requestRepository.findByIdAndDeletedFalse(requestId)).thenReturn(Optional.of(request));
        when(userRepository.findById(userId)).thenReturn(Optional.of(buildUser()));
        when(requestRepository.save(any())).thenReturn(request);
        when(requestMapper.toResponse(any())).thenReturn(new QaServiceRequestResponse());

        qaRequestService.transition(requestId, "COMPLETED", userId, "Done");

        assertNotNull(request.getResolvedAt());
    }

    @Test
    void delete_shouldSoftDelete() {
        QaServiceRequest request = buildRequest(QaServiceRequest.RequestStatus.SUBMITTED);

        when(requestRepository.findByIdAndDeletedFalse(requestId)).thenReturn(Optional.of(request));
        when(requestRepository.save(any())).thenReturn(request);

        qaRequestService.delete(requestId);

        assertTrue(request.isDeleted());
        verify(requestRepository).save(request);
    }

    @Test
    void delete_shouldThrowWhenNotFound() {
        when(requestRepository.findByIdAndDeletedFalse(requestId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> qaRequestService.delete(requestId));
    }

    @Test
    void getByStatus_shouldFilterCorrectly() {
        Pageable pageable = PageRequest.of(0, 10);
        QaServiceRequest request = buildRequest(QaServiceRequest.RequestStatus.IN_PROGRESS);
        Page<QaServiceRequest> page = new PageImpl<>(List.of(request));
        QaServiceRequestResponse response = new QaServiceRequestResponse();

        when(requestRepository.findByStatusAndDeletedFalse(QaServiceRequest.RequestStatus.IN_PROGRESS, pageable))
                .thenReturn(page);
        when(requestMapper.toResponse(request)).thenReturn(response);

        Page<QaServiceRequestResponse> result = qaRequestService.getByStatus("IN_PROGRESS", pageable);

        assertEquals(1, result.getTotalElements());
        verify(requestRepository).findByStatusAndDeletedFalse(QaServiceRequest.RequestStatus.IN_PROGRESS, pageable);
    }

    @Test
    void getByTeam_shouldReturnRequestsForTeam() {
        Pageable pageable = PageRequest.of(0, 10);
        QaServiceRequest request = buildRequest(QaServiceRequest.RequestStatus.SUBMITTED);
        Page<QaServiceRequest> page = new PageImpl<>(List.of(request));
        QaServiceRequestResponse response = new QaServiceRequestResponse();

        when(requestRepository.findByRequestingTeamIdAndDeletedFalse(teamId, pageable)).thenReturn(page);
        when(requestMapper.toResponse(request)).thenReturn(response);

        Page<QaServiceRequestResponse> result = qaRequestService.getByTeam(teamId, pageable);

        assertEquals(1, result.getTotalElements());
        verify(requestRepository).findByRequestingTeamIdAndDeletedFalse(teamId, pageable);
    }

    @Test
    void create_shouldUsePrioritizationEngine() {
        CreateQaServiceRequestRequest req = buildCreateRequest();
        req.setImpact("CRITICAL");
        req.setUrgency("IMMEDIATE");

        QaService qaService = buildQaService();
        Team team = buildTeam();
        User user = buildUser();

        when(organizationRepository.findById(DEFAULT_ORG_ID)).thenReturn(Optional.of(org));
        when(qaServiceRepository.findByIdAndDeletedFalse(qaServiceId)).thenReturn(Optional.of(qaService));
        when(teamRepository.findByIdAndDeletedFalse(teamId)).thenReturn(Optional.of(team));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(prioritizationEngine.computePriority(QaServiceRequest.Impact.CRITICAL, QaServiceRequest.Urgency.IMMEDIATE))
                .thenReturn(1);
        when(requestRepository.getNextRequestNumber()).thenReturn(3L);
        when(slaConfigurationRepository.findByPriorityLevelAndOrganizationIdAndDeletedFalse(1, DEFAULT_ORG_ID))
                .thenReturn(Optional.empty());
        when(requestRepository.save(any(QaServiceRequest.class))).thenAnswer(inv -> {
            QaServiceRequest saved = inv.getArgument(0);
            assertEquals(1, saved.getPriorityScore());
            return saved;
        });
        when(requestMapper.toResponse(any())).thenReturn(new QaServiceRequestResponse());

        qaRequestService.create(req, userId);

        verify(prioritizationEngine).computePriority(QaServiceRequest.Impact.CRITICAL, QaServiceRequest.Urgency.IMMEDIATE);
    }

    @Test
    void create_shouldFallbackToServiceEstimatedHoursWhenRequestHoursNull() {
        CreateQaServiceRequestRequest req = buildCreateRequest();
        req.setEstimatedHours(null); // No override

        QaService qaService = buildQaService();
        qaService.setEstimatedHours(new BigDecimal("8.0"));
        Team team = buildTeam();
        User user = buildUser();

        when(organizationRepository.findById(DEFAULT_ORG_ID)).thenReturn(Optional.of(org));
        when(qaServiceRepository.findByIdAndDeletedFalse(qaServiceId)).thenReturn(Optional.of(qaService));
        when(teamRepository.findByIdAndDeletedFalse(teamId)).thenReturn(Optional.of(team));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(prioritizationEngine.computePriority(any(), any())).thenReturn(2);
        when(requestRepository.getNextRequestNumber()).thenReturn(4L);
        when(slaConfigurationRepository.findByPriorityLevelAndOrganizationIdAndDeletedFalse(anyInt(), any()))
                .thenReturn(Optional.empty());
        when(requestRepository.save(any(QaServiceRequest.class))).thenAnswer(inv -> {
            QaServiceRequest saved = inv.getArgument(0);
            assertEquals(new BigDecimal("8.0"), saved.getEstimatedHours());
            return saved;
        });
        when(requestMapper.toResponse(any())).thenReturn(new QaServiceRequestResponse());

        qaRequestService.create(req, userId);

        verify(requestRepository, times(2)).save(any(QaServiceRequest.class));
    }

    @Test
    void getAll_shouldUseSoftDeleteAwarePagination() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<QaServiceRequest> page = new PageImpl<>(List.of());

        when(requestRepository.findByDeletedFalse(pageable)).thenReturn(page);

        Page<QaServiceRequestResponse> result = qaRequestService.getAll(pageable);

        assertEquals(0, result.getTotalElements());
        verify(requestRepository).findByDeletedFalse(pageable);
        verify(requestRepository, never()).findAll(any(Pageable.class));
    }
}
