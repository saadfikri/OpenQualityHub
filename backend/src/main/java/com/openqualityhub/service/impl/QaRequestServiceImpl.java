package com.openqualityhub.service.impl;

import com.openqualityhub.dto.request.CreateQaServiceRequestRequest;
import com.openqualityhub.dto.request.OverridePriorityRequest;
import com.openqualityhub.dto.request.UpdateQaServiceRequestRequest;
import com.openqualityhub.dto.response.QaRequestDashboardResponse;
import com.openqualityhub.dto.response.QaServiceRequestResponse;
import com.openqualityhub.entity.*;
import com.openqualityhub.exception.InvalidStateTransitionException;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.mapper.QaServiceRequestMapper;
import com.openqualityhub.repository.*;
import com.openqualityhub.service.QaRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QaRequestServiceImpl implements QaRequestService {

    private static final UUID DEFAULT_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final QaServiceRequestRepository requestRepository;
    private final QaServiceRepository qaServiceRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;
    private final SlaConfigurationRepository slaConfigurationRepository;
    private final QaServiceRequestMapper requestMapper;
    private final PrioritizationEngine prioritizationEngine;

    @Override
    @Transactional
    public QaServiceRequestResponse create(CreateQaServiceRequestRequest req, UUID requestingUserId) {
        Organization org = organizationRepository.findById(DEFAULT_ORG_ID).orElseThrow();
        QaService qaService = qaServiceRepository.findByIdAndDeletedFalse(req.getQaServiceId())
            .orElseThrow(() -> new ResourceNotFoundException("QaService", req.getQaServiceId()));
        Team requestingTeam = teamRepository.findByIdAndDeletedFalse(req.getRequestingTeamId())
            .orElseThrow(() -> new ResourceNotFoundException("Team", req.getRequestingTeamId()));
        User requestingUser = userRepository.findById(requestingUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User", requestingUserId));

        QaServiceRequest.Impact impact = QaServiceRequest.Impact.valueOf(req.getImpact());
        QaServiceRequest.Urgency urgency = QaServiceRequest.Urgency.valueOf(req.getUrgency());
        int priorityScore = prioritizationEngine.computePriority(impact, urgency);

        long seqNum = requestRepository.getNextRequestNumber();
        String requestNumber = String.format("QA-%04d", seqNum);

        QaServiceRequest request = QaServiceRequest.builder()
            .organization(org)
            .requestNumber(requestNumber)
            .title(req.getTitle())
            .description(req.getDescription())
            .qaService(qaService)
            .requestingTeam(requestingTeam)
            .requestingUser(requestingUser)
            .status(QaServiceRequest.RequestStatus.SUBMITTED)
            .impact(impact)
            .urgency(urgency)
            .priorityScore(priorityScore)
            .estimatedHours(req.getEstimatedHours() != null ? req.getEstimatedHours() : qaService.getEstimatedHours())
            .build();

        if (req.getProjectId() != null) {
            request.setProject(projectRepository.findByIdAndDeletedFalse(req.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", req.getProjectId())));
        }

        // Bind SLA based on computed priority
        slaConfigurationRepository.findByPriorityLevelAndOrganizationIdAndDeletedFalse(priorityScore, DEFAULT_ORG_ID)
            .ifPresent(sla -> {
                request.setSlaConfiguration(sla);
                request.setResponseDeadline(Instant.now().plus(sla.getResponseTimeHours(), ChronoUnit.HOURS));
                request.setResolutionDeadline(Instant.now().plus(sla.getResolutionTimeHours(), ChronoUnit.HOURS));
            });

        QaServiceRequest saved = requestRepository.save(request);

        addActivity(saved, requestingUser, QaRequestActivity.ActivityType.STATUS_CHANGE,
            null, "SUBMITTED", "Request created");

        saved = requestRepository.save(saved);
        log.info("Created QA request: {} ({})", saved.getRequestNumber(), saved.getId());
        return requestMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public QaServiceRequestResponse update(UUID id, UpdateQaServiceRequestRequest req, UUID actingUserId) {
        QaServiceRequest request = findRequest(id);
        User actor = actingUserId != null ? userRepository.findById(actingUserId).orElse(null) : null;

        if (req.getTitle() != null) request.setTitle(req.getTitle());
        if (req.getDescription() != null) request.setDescription(req.getDescription());
        if (req.getEstimatedHours() != null) request.setEstimatedHours(req.getEstimatedHours());
        if (req.getActualHours() != null) {
            request.setActualHours(req.getActualHours());
            addActivity(request, actor, QaRequestActivity.ActivityType.HOURS_LOGGED,
                null, req.getActualHours().toString(), null);
        }

        if (req.getAssignedTeamId() != null) {
            Team team = teamRepository.findByIdAndDeletedFalse(req.getAssignedTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team", req.getAssignedTeamId()));
            request.setAssignedTeam(team);
            addActivity(request, actor, QaRequestActivity.ActivityType.ASSIGNMENT,
                null, team.getName(), "Assigned to team");
        }

        if (req.getAssignedUserId() != null) {
            User assignee = userRepository.findById(req.getAssignedUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.getAssignedUserId()));
            request.setAssignedUser(assignee);
            addActivity(request, actor, QaRequestActivity.ActivityType.ASSIGNMENT,
                null, assignee.getDisplayName(), "Assigned to user");
        }

        if (req.getStatus() != null) {
            return transition(id, req.getStatus(), actingUserId, req.getComment());
        }

        if (req.getComment() != null && req.getStatus() == null) {
            addActivity(request, actor, QaRequestActivity.ActivityType.COMMENT,
                null, null, req.getComment());
        }

        request = requestRepository.save(request);
        return requestMapper.toResponse(request);
    }

    @Override
    @Transactional
    public QaServiceRequestResponse transition(UUID id, String targetStatus, UUID actingUserId, String comment) {
        QaServiceRequest request = findRequest(id);
        User actor = actingUserId != null ? userRepository.findById(actingUserId).orElse(null) : null;

        QaServiceRequest.RequestStatus from = request.getStatus();
        QaServiceRequest.RequestStatus to = QaServiceRequest.RequestStatus.valueOf(targetStatus);

        if (!from.canTransitionTo(to)) {
            throw new InvalidStateTransitionException(from.name(), to.name());
        }

        String oldStatus = from.name();
        request.setStatus(to);

        // Record timestamps for SLA tracking
        if (to == QaServiceRequest.RequestStatus.TRIAGED && request.getRespondedAt() == null) {
            request.setRespondedAt(Instant.now());
        }
        if (to == QaServiceRequest.RequestStatus.COMPLETED && request.getResolvedAt() == null) {
            request.setResolvedAt(Instant.now());
        }

        addActivity(request, actor, QaRequestActivity.ActivityType.STATUS_CHANGE,
            oldStatus, to.name(), comment);

        request = requestRepository.save(request);
        log.info("Transitioned QA request {} from {} to {}", request.getRequestNumber(), oldStatus, to.name());
        return requestMapper.toResponse(request);
    }

    @Override
    @Transactional
    public QaServiceRequestResponse overridePriority(UUID id, OverridePriorityRequest req, UUID actingUserId) {
        QaServiceRequest request = findRequest(id);
        User actor = actingUserId != null ? userRepository.findById(actingUserId).orElse(null) : null;

        String oldPriority = String.valueOf(request.getEffectivePriority());
        request.setPriorityOverride(req.getPriority());
        request.setOverrideReason(req.getReason());

        // Re-bind SLA to overridden priority
        var slaOpt = slaConfigurationRepository.findByPriorityLevelAndOrganizationIdAndDeletedFalse(req.getPriority(), DEFAULT_ORG_ID);
        if (slaOpt.isPresent()) {
            var sla = slaOpt.get();
            request.setSlaConfiguration(sla);
            if (request.getRespondedAt() == null) {
                request.setResponseDeadline(request.getCreatedAt().plus(sla.getResponseTimeHours(), ChronoUnit.HOURS));
            }
            request.setResolutionDeadline(request.getCreatedAt().plus(sla.getResolutionTimeHours(), ChronoUnit.HOURS));
        }

        addActivity(request, actor, QaRequestActivity.ActivityType.PRIORITY_OVERRIDE,
            oldPriority, String.valueOf(req.getPriority()), req.getReason());

        request = requestRepository.save(request);
        log.info("Priority override on {}: {} → {} (reason: {})",
            request.getRequestNumber(), oldPriority, req.getPriority(), req.getReason());
        return requestMapper.toResponse(request);
    }

    @Override
    @Transactional
    public QaServiceRequestResponse addComment(UUID id, String comment, UUID userId) {
        QaServiceRequest request = findRequest(id);
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        addActivity(request, user, QaRequestActivity.ActivityType.COMMENT, null, null, comment);
        request = requestRepository.save(request);
        return requestMapper.toResponse(request);
    }

    @Override
    public QaServiceRequestResponse getById(UUID id) {
        return requestMapper.toResponse(findRequest(id));
    }

    @Override
    public Page<QaServiceRequestResponse> getAll(Pageable pageable) {
        return requestRepository.findByDeletedFalse(pageable).map(requestMapper::toResponse);
    }

    @Override
    public Page<QaServiceRequestResponse> getByStatus(String status, Pageable pageable) {
        return requestRepository.findByStatusAndDeletedFalse(
            QaServiceRequest.RequestStatus.valueOf(status), pageable
        ).map(requestMapper::toResponse);
    }

    @Override
    public Page<QaServiceRequestResponse> getByTeam(UUID teamId, Pageable pageable) {
        return requestRepository.findByRequestingTeamIdAndDeletedFalse(teamId, pageable)
            .map(requestMapper::toResponse);
    }

    @Override
    public Page<QaServiceRequestResponse> getByAssignee(UUID userId, Pageable pageable) {
        return requestRepository.findByAssignedUserIdAndDeletedFalse(userId, pageable)
            .map(requestMapper::toResponse);
    }

    @Override
    public QaRequestDashboardResponse getDashboard() {
        List<QaRequestDashboardResponse.StatusCount> statusCounts = Arrays.stream(QaServiceRequest.RequestStatus.values())
            .map(s -> QaRequestDashboardResponse.StatusCount.builder()
                .status(s.name())
                .count(requestRepository.countByStatusAndDeletedFalse(s))
                .build())
            .collect(Collectors.toList());

        long open = statusCounts.stream()
            .filter(s -> Set.of("SUBMITTED", "TRIAGED", "ASSIGNED").contains(s.getStatus()))
            .mapToLong(QaRequestDashboardResponse.StatusCount::getCount).sum();
        long inProgress = statusCounts.stream()
            .filter(s -> Set.of("IN_PROGRESS", "ON_HOLD").contains(s.getStatus()))
            .mapToLong(QaRequestDashboardResponse.StatusCount::getCount).sum();
        long completed = statusCounts.stream()
            .filter(s -> Set.of("COMPLETED", "CLOSED").contains(s.getStatus()))
            .mapToLong(QaRequestDashboardResponse.StatusCount::getCount).sum();
        long cancelled = statusCounts.stream()
            .filter(s -> "CANCELLED".equals(s.getStatus()))
            .mapToLong(QaRequestDashboardResponse.StatusCount::getCount).sum();
        long total = statusCounts.stream().mapToLong(QaRequestDashboardResponse.StatusCount::getCount).sum();

        // Priority distribution (using effective priority)
        List<QaRequestDashboardResponse.PriorityCount> priorityCounts = new ArrayList<>();
        for (int p = 1; p <= 5; p++) {
            final int priority = p;
            long count = requestRepository.findByDeletedFalse(Pageable.unpaged()).stream()
                .filter(r -> r.getEffectivePriority() == priority)
                .count();
            priorityCounts.add(QaRequestDashboardResponse.PriorityCount.builder()
                .priority(p).count(count).build());
        }

        // Requests by service
        Map<UUID, long[]> serviceCountMap = new LinkedHashMap<>();
        requestRepository.findByDeletedFalse(Pageable.unpaged()).forEach(r -> {
            UUID svcId = r.getQaService().getId();
            serviceCountMap.computeIfAbsent(svcId, k -> new long[]{0})[0]++;
        });
        List<QaRequestDashboardResponse.ServiceCount> serviceCounts = serviceCountMap.entrySet().stream()
            .map(e -> {
                var svc = qaServiceRepository.findByIdAndDeletedFalse(e.getKey()).orElse(null);
                return QaRequestDashboardResponse.ServiceCount.builder()
                    .serviceName(svc != null ? svc.getName() : "Unknown")
                    .category(svc != null && svc.getCategory() != null ? svc.getCategory().name() : null)
                    .count(e.getValue()[0])
                    .build();
            })
            .collect(Collectors.toList());

        return QaRequestDashboardResponse.builder()
            .totalRequests(total)
            .openRequests(open)
            .inProgressRequests(inProgress)
            .completedRequests(completed)
            .cancelledRequests(cancelled)
            .slaBreachCount(requestRepository.findBreachedRequests(Pageable.unpaged()).getTotalElements())
            .requestsByStatus(statusCounts)
            .requestsByPriority(priorityCounts)
            .requestsByService(serviceCounts)
            .build();
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        QaServiceRequest request = findRequest(id);
        request.setDeleted(true);
        requestRepository.save(request);
        log.info("Soft-deleted QA request: {}", id);
    }

    private QaServiceRequest findRequest(UUID id) {
        return requestRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("QaServiceRequest", id));
    }

    private void addActivity(QaServiceRequest request, User user,
                             QaRequestActivity.ActivityType type,
                             String oldValue, String newValue, String comment) {
        QaRequestActivity activity = QaRequestActivity.builder()
            .request(request)
            .user(user)
            .activityType(type)
            .oldValue(oldValue)
            .newValue(newValue)
            .comment(comment)
            .build();
        request.getActivities().add(activity);
    }
}
