package com.openqualityhub.mapper;

import com.openqualityhub.dto.response.QaServiceRequestResponse;
import com.openqualityhub.entity.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class QaServiceRequestMapperTest {

    private final QaServiceRequestMapper mapper = Mappers.getMapper(QaServiceRequestMapper.class);

    @Test
    void toResponse_shouldMapAllFields() {
        QaService qaService = QaService.builder()
            .name("Security Testing")
            .category(QaService.ServiceCategory.SECURITY_TESTING)
            .build();
        qaService.setId(UUID.randomUUID());

        Team requestingTeam = Team.builder().name("Frontend Team").build();
        requestingTeam.setId(UUID.randomUUID());

        User requestingUser = User.builder().displayName("Alice Smith").build();
        requestingUser.setId(UUID.randomUUID());

        Project project = Project.builder().name("Web App").build();
        project.setId(UUID.randomUUID());

        Team assignedTeam = Team.builder().name("QA Team").build();
        assignedTeam.setId(UUID.randomUUID());

        User assignedUser = User.builder().displayName("Bob Jones").build();
        assignedUser.setId(UUID.randomUUID());

        QaServiceRequest request = QaServiceRequest.builder()
            .requestNumber("QA-001")
            .title("Security audit needed")
            .description("Full security audit for web app")
            .qaService(qaService)
            .requestingTeam(requestingTeam)
            .requestingUser(requestingUser)
            .project(project)
            .assignedTeam(assignedTeam)
            .assignedUser(assignedUser)
            .status(QaServiceRequest.RequestStatus.IN_PROGRESS)
            .impact(QaServiceRequest.Impact.HIGH)
            .urgency(QaServiceRequest.Urgency.IMMEDIATE)
            .priorityScore(5)
            .estimatedHours(new BigDecimal("16.0"))
            .actualHours(new BigDecimal("8.5"))
            .activities(new ArrayList<>())
            .build();
        request.setId(UUID.randomUUID());
        request.setCreatedAt(Instant.parse("2024-07-01T09:00:00Z"));
        request.setUpdatedAt(Instant.parse("2024-07-05T15:00:00Z"));

        QaServiceRequestResponse response = mapper.toResponse(request);

        assertEquals(request.getId().toString(), response.getId());
        assertEquals("QA-001", response.getRequestNumber());
        assertEquals("Security audit needed", response.getTitle());
        assertEquals("Full security audit for web app", response.getDescription());
        assertEquals("IN_PROGRESS", response.getStatus());
        assertEquals("HIGH", response.getImpact());
        assertEquals("IMMEDIATE", response.getUrgency());
        assertEquals(5, response.getPriorityScore());
        assertEquals(5, response.getEffectivePriority());

        assertEquals(qaService.getId().toString(), response.getQaServiceId());
        assertEquals("Security Testing", response.getQaServiceName());
        assertEquals("SECURITY_TESTING", response.getQaServiceCategory());

        assertEquals(requestingTeam.getId().toString(), response.getRequestingTeamId());
        assertEquals("Frontend Team", response.getRequestingTeamName());
        assertEquals(requestingUser.getId().toString(), response.getRequestingUserId());
        assertEquals("Alice Smith", response.getRequestingUserName());

        assertEquals(project.getId().toString(), response.getProjectId());
        assertEquals("Web App", response.getProjectName());

        assertEquals(assignedTeam.getId().toString(), response.getAssignedTeamId());
        assertEquals("QA Team", response.getAssignedTeamName());
        assertEquals(assignedUser.getId().toString(), response.getAssignedUserId());
        assertEquals("Bob Jones", response.getAssignedUserName());

        assertEquals(new BigDecimal("16.0"), response.getEstimatedHours());
        assertEquals(new BigDecimal("8.5"), response.getActualHours());
    }

    @Test
    void toResponse_shouldHandleNullOptionalRelations() {
        QaService qaService = QaService.builder()
            .name("Test Planning")
            .category(QaService.ServiceCategory.TEST_PLANNING)
            .build();
        qaService.setId(UUID.randomUUID());

        Team requestingTeam = Team.builder().name("Dev Team").build();
        requestingTeam.setId(UUID.randomUUID());

        User requestingUser = User.builder().displayName("Charlie").build();
        requestingUser.setId(UUID.randomUUID());

        QaServiceRequest request = QaServiceRequest.builder()
            .requestNumber("QA-002")
            .title("Need test plan")
            .qaService(qaService)
            .requestingTeam(requestingTeam)
            .requestingUser(requestingUser)
            .project(null)
            .assignedTeam(null)
            .assignedUser(null)
            .activities(new ArrayList<>())
            .build();
        request.setId(UUID.randomUUID());

        QaServiceRequestResponse response = mapper.toResponse(request);

        assertNull(response.getProjectId());
        assertNull(response.getProjectName());
        assertNull(response.getAssignedTeamId());
        assertNull(response.getAssignedTeamName());
        assertNull(response.getAssignedUserId());
        assertNull(response.getAssignedUserName());
    }

    @Test
    void toResponse_shouldMapPriorityOverride() {
        QaService qaService = QaService.builder()
            .name("Regression")
            .category(QaService.ServiceCategory.REGRESSION_TESTING)
            .build();
        qaService.setId(UUID.randomUUID());

        Team team = Team.builder().name("QA").build();
        team.setId(UUID.randomUUID());

        User user = User.builder().displayName("Dan").build();
        user.setId(UUID.randomUUID());

        QaServiceRequest request = QaServiceRequest.builder()
            .requestNumber("QA-003")
            .title("Regression run")
            .qaService(qaService)
            .requestingTeam(team)
            .requestingUser(user)
            .priorityScore(3)
            .priorityOverride(1)
            .overrideReason("Critical release blocker")
            .activities(new ArrayList<>())
            .build();
        request.setId(UUID.randomUUID());

        QaServiceRequestResponse response = mapper.toResponse(request);

        assertEquals(3, response.getPriorityScore());
        assertEquals(1, response.getPriorityOverride());
        assertEquals(1, response.getEffectivePriority());
        assertEquals("Critical release blocker", response.getOverrideReason());
    }

    @Test
    void toResponse_shouldMapDeadlinesAndTimestamps() {
        QaService qaService = QaService.builder().name("Test").build();
        qaService.setId(UUID.randomUUID());

        Team team = Team.builder().name("T").build();
        team.setId(UUID.randomUUID());

        User user = User.builder().displayName("U").build();
        user.setId(UUID.randomUUID());

        Instant responseDeadline = Instant.parse("2024-08-01T12:00:00Z");
        Instant resolutionDeadline = Instant.parse("2024-08-05T12:00:00Z");
        Instant respondedAt = Instant.parse("2024-07-31T10:00:00Z");
        Instant resolvedAt = Instant.parse("2024-08-03T16:00:00Z");

        QaServiceRequest request = QaServiceRequest.builder()
            .requestNumber("QA-004")
            .title("Timed request")
            .qaService(qaService)
            .requestingTeam(team)
            .requestingUser(user)
            .responseDeadline(responseDeadline)
            .resolutionDeadline(resolutionDeadline)
            .respondedAt(respondedAt)
            .resolvedAt(resolvedAt)
            .activities(new ArrayList<>())
            .build();
        request.setId(UUID.randomUUID());
        request.setCreatedAt(Instant.parse("2024-07-30T08:00:00Z"));
        request.setUpdatedAt(Instant.parse("2024-08-03T16:00:00Z"));

        QaServiceRequestResponse response = mapper.toResponse(request);

        assertEquals(responseDeadline.toString(), response.getResponseDeadline());
        assertEquals(resolutionDeadline.toString(), response.getResolutionDeadline());
        assertEquals(respondedAt.toString(), response.getRespondedAt());
        assertEquals(resolvedAt.toString(), response.getResolvedAt());
        assertEquals("2024-07-30T08:00:00Z", response.getCreatedAt());
        assertEquals("2024-08-03T16:00:00Z", response.getUpdatedAt());
    }

    @Test
    void toActivityResponse_shouldMapFields() {
        User user = User.builder().displayName("Eve").build();
        user.setId(UUID.randomUUID());

        QaRequestActivity activity = QaRequestActivity.builder()
            .user(user)
            .activityType(QaRequestActivity.ActivityType.STATUS_CHANGE)
            .oldValue("SUBMITTED")
            .newValue("TRIAGED")
            .comment("Reviewed and triaged")
            .build();
        activity.setId(UUID.randomUUID());
        activity.setCreatedAt(Instant.parse("2024-07-02T11:00:00Z"));

        QaServiceRequestResponse.ActivityResponse response = mapper.toActivityResponse(activity);

        assertEquals(activity.getId().toString(), response.getId());
        assertEquals(user.getId().toString(), response.getUserId());
        assertEquals("Eve", response.getUserName());
        assertEquals("STATUS_CHANGE", response.getActivityType());
        assertEquals("SUBMITTED", response.getOldValue());
        assertEquals("TRIAGED", response.getNewValue());
        assertEquals("Reviewed and triaged", response.getComment());
        assertEquals("2024-07-02T11:00:00Z", response.getCreatedAt());
    }

    @Test
    void toResponse_shouldReturnNullForNullInput() {
        assertNull(mapper.toResponse(null));
    }
}
