package com.openqualityhub.service.impl;

import com.openqualityhub.dto.request.CreateCapacityAllocationRequest;
import com.openqualityhub.dto.response.CapacityOverviewResponse;
import com.openqualityhub.entity.CapacityAllocation;
import com.openqualityhub.entity.Team;
import com.openqualityhub.entity.User;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.repository.CapacityAllocationRepository;
import com.openqualityhub.repository.QaServiceRequestRepository;
import com.openqualityhub.repository.TeamRepository;
import com.openqualityhub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CapacityServiceImplTest {

    @Mock private CapacityAllocationRepository capacityRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private UserRepository userRepository;
    @Mock private QaServiceRequestRepository requestRepository;

    @InjectMocks
    private CapacityServiceImpl capacityService;

    private UUID teamId;
    private UUID userId;
    private LocalDate weekStart;

    @BeforeEach
    void setUp() {
        teamId = UUID.randomUUID();
        userId = UUID.randomUUID();
        weekStart = LocalDate.of(2026, 11, 9); // Monday
    }

    private Team buildTeam() {
        Team team = Team.builder().name("QA Core").build();
        team.setId(teamId);
        return team;
    }

    private User buildUser() {
        User user = User.builder()
                .firstName("Jane")
                .lastName("Smith")
                .displayName("Jane Smith")
                .email("jane@test.com")
                .passwordHash("hash")
                .build();
        user.setId(userId);
        return user;
    }

    @Test
    void createOrUpdate_shouldCreateNewTeamAllocation() {
        CreateCapacityAllocationRequest req = new CreateCapacityAllocationRequest();
        req.setTeamId(teamId);
        req.setWeekStart(weekStart);
        req.setTotalHours(new BigDecimal("160.0"));
        req.setAllocatedHours(new BigDecimal("120.0"));
        req.setNotes("Sprint 42");

        Team team = buildTeam();

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(capacityRepository.findByTeamIdAndUserIsNullAndWeekStartAndDeletedFalse(teamId, weekStart))
                .thenReturn(Optional.empty());

        capacityService.createOrUpdate(req);

        verify(capacityRepository).save(argThat(alloc -> {
            CapacityAllocation a = (CapacityAllocation) alloc;
            return a.getTeam().equals(team)
                    && a.getTotalHours().equals(new BigDecimal("160.0"))
                    && a.getAllocatedHours().equals(new BigDecimal("120.0"))
                    && "Sprint 42".equals(a.getNotes());
        }));
    }

    @Test
    void createOrUpdate_shouldUpdateExistingTeamAllocation() {
        CreateCapacityAllocationRequest req = new CreateCapacityAllocationRequest();
        req.setTeamId(teamId);
        req.setWeekStart(weekStart);
        req.setTotalHours(new BigDecimal("160.0"));
        req.setAllocatedHours(new BigDecimal("140.0"));

        Team team = buildTeam();
        CapacityAllocation existing = CapacityAllocation.builder()
                .team(team)
                .weekStart(weekStart)
                .totalHours(new BigDecimal("160.0"))
                .allocatedHours(new BigDecimal("100.0"))
                .build();

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(capacityRepository.findByTeamIdAndUserIsNullAndWeekStartAndDeletedFalse(teamId, weekStart))
                .thenReturn(Optional.of(existing));

        capacityService.createOrUpdate(req);

        assertEquals(new BigDecimal("140.0"), existing.getAllocatedHours());
        verify(capacityRepository).save(existing);
    }

    @Test
    void createOrUpdate_shouldCreateUserAllocation() {
        CreateCapacityAllocationRequest req = new CreateCapacityAllocationRequest();
        req.setTeamId(teamId);
        req.setUserId(userId);
        req.setWeekStart(weekStart);
        req.setTotalHours(new BigDecimal("40.0"));
        req.setAllocatedHours(new BigDecimal("32.0"));

        Team team = buildTeam();
        User user = buildUser();

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(capacityRepository.findByUserIdAndWeekStartAndDeletedFalse(userId, weekStart))
                .thenReturn(Optional.empty());

        capacityService.createOrUpdate(req);

        verify(capacityRepository).save(argThat(alloc -> {
            CapacityAllocation a = (CapacityAllocation) alloc;
            return a.getUser() != null && a.getUser().equals(user)
                    && a.getTeam().equals(team);
        }));
    }

    @Test
    void createOrUpdate_shouldThrowWhenTeamNotFound() {
        CreateCapacityAllocationRequest req = new CreateCapacityAllocationRequest();
        req.setTeamId(teamId);
        req.setWeekStart(weekStart);
        req.setTotalHours(new BigDecimal("40.0"));
        req.setAllocatedHours(new BigDecimal("20.0"));

        when(teamRepository.findById(teamId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> capacityService.createOrUpdate(req));
    }

    @Test
    void createOrUpdate_shouldThrowWhenUserNotFound() {
        CreateCapacityAllocationRequest req = new CreateCapacityAllocationRequest();
        req.setTeamId(teamId);
        req.setUserId(userId);
        req.setWeekStart(weekStart);
        req.setTotalHours(new BigDecimal("40.0"));
        req.setAllocatedHours(new BigDecimal("20.0"));

        Team team = buildTeam();
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> capacityService.createOrUpdate(req));
    }

    @Test
    void getOverview_shouldReturnCapacityForAllTeams() {
        Team team = buildTeam();

        CapacityAllocation alloc = CapacityAllocation.builder()
                .team(team)
                .weekStart(weekStart)
                .totalHours(new BigDecimal("160.0"))
                .allocatedHours(new BigDecimal("120.0"))
                .build();

        when(teamRepository.findByDeletedFalse()).thenReturn(List.of(team));
        when(capacityRepository.findByTeamIdAndWeekStartBetweenAndDeletedFalse(teamId, weekStart, weekStart))
                .thenReturn(List.of(alloc));
        when(requestRepository.countActiveByAssignedTeam(teamId)).thenReturn(5L);
        when(requestRepository.sumEstimatedHoursByAssignedTeam(teamId)).thenReturn(new BigDecimal("40.0"));

        CapacityOverviewResponse result = capacityService.getOverview(weekStart);

        assertNotNull(result);
        assertEquals(1, result.getTeams().size());
        CapacityOverviewResponse.TeamCapacity tc = result.getTeams().get(0);
        assertEquals("QA Core", tc.getTeamName());
        assertEquals(new BigDecimal("160.0"), tc.getTotalHours());
        assertEquals(new BigDecimal("120.0"), tc.getAllocatedHours());
        assertEquals(new BigDecimal("40.0"), tc.getAvailableHours());
        assertEquals(5L, tc.getActiveRequests());
        assertEquals("HIGH", tc.getUtilizationLevel()); // 120/160 = 0.75 => HIGH
    }

    @Test
    void getOverview_shouldReturnOverloadedWhenUtilizationAbove90Percent() {
        Team team = buildTeam();

        CapacityAllocation alloc = CapacityAllocation.builder()
                .team(team)
                .weekStart(weekStart)
                .totalHours(new BigDecimal("100.0"))
                .allocatedHours(new BigDecimal("95.0"))
                .build();

        when(teamRepository.findByDeletedFalse()).thenReturn(List.of(team));
        when(capacityRepository.findByTeamIdAndWeekStartBetweenAndDeletedFalse(teamId, weekStart, weekStart))
                .thenReturn(List.of(alloc));
        when(requestRepository.countActiveByAssignedTeam(teamId)).thenReturn(0L);
        when(requestRepository.sumEstimatedHoursByAssignedTeam(teamId)).thenReturn(BigDecimal.ZERO);

        CapacityOverviewResponse result = capacityService.getOverview(weekStart);

        assertEquals("OVERLOADED", result.getTeams().get(0).getUtilizationLevel());
    }

    @Test
    void getOverview_shouldReturnNoCapacityWhenTotalIsZero() {
        Team team = buildTeam();

        when(teamRepository.findByDeletedFalse()).thenReturn(List.of(team));
        when(capacityRepository.findByTeamIdAndWeekStartBetweenAndDeletedFalse(teamId, weekStart, weekStart))
                .thenReturn(List.of()); // No allocations
        when(requestRepository.countActiveByAssignedTeam(teamId)).thenReturn(0L);
        when(requestRepository.sumEstimatedHoursByAssignedTeam(teamId)).thenReturn(BigDecimal.ZERO);

        CapacityOverviewResponse result = capacityService.getOverview(weekStart);

        assertEquals("NO_CAPACITY", result.getTeams().get(0).getUtilizationLevel());
    }

    @Test
    void getOverview_shouldDefaultToCurrentMondayWhenWeekStartNull() {
        when(teamRepository.findByDeletedFalse()).thenReturn(List.of());

        CapacityOverviewResponse result = capacityService.getOverview(null);

        assertNotNull(result);
        assertEquals(0, result.getTeams().size());
        verify(teamRepository).findByDeletedFalse();
    }
}
