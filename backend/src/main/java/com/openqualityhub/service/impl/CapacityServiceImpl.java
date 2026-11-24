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
import com.openqualityhub.service.CapacityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CapacityServiceImpl implements CapacityService {

    private final CapacityAllocationRepository capacityRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final QaServiceRequestRepository requestRepository;

    @Override
    @Transactional
    public void createOrUpdate(CreateCapacityAllocationRequest request) {
        Team team = teamRepository.findById(request.getTeamId())
            .orElseThrow(() -> new ResourceNotFoundException("Team", request.getTeamId()));

        CapacityAllocation allocation;
        if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));
            allocation = capacityRepository.findByUserIdAndWeekStartAndDeletedFalse(request.getUserId(), request.getWeekStart())
                .orElseGet(() -> CapacityAllocation.builder().team(team).user(user).weekStart(request.getWeekStart()).build());
        } else {
            allocation = capacityRepository.findByTeamIdAndUserIsNullAndWeekStartAndDeletedFalse(request.getTeamId(), request.getWeekStart())
                .orElseGet(() -> CapacityAllocation.builder().team(team).weekStart(request.getWeekStart()).build());
        }

        allocation.setTotalHours(request.getTotalHours());
        allocation.setAllocatedHours(request.getAllocatedHours());
        if (request.getNotes() != null) allocation.setNotes(request.getNotes());

        capacityRepository.save(allocation);
        log.info("Capacity allocation saved for team {} week {}", team.getName(), request.getWeekStart());
    }

    @Override
    public CapacityOverviewResponse getOverview(LocalDate weekStart) {
        if (weekStart == null) {
            weekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        }

        List<Team> teams = teamRepository.findByDeletedFalse();
        List<CapacityOverviewResponse.TeamCapacity> teamCapacities = new ArrayList<>();
        BigDecimal totalAvailable = BigDecimal.ZERO;
        BigDecimal totalAllocated = BigDecimal.ZERO;
        long totalActiveRequests = 0;

        for (Team team : teams) {
            List<CapacityAllocation> allocations = capacityRepository
                .findByTeamIdAndWeekStartBetweenAndDeletedFalse(team.getId(), weekStart, weekStart);

            BigDecimal teamTotal = allocations.stream()
                .map(CapacityAllocation::getTotalHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal teamAllocated = allocations.stream()
                .map(CapacityAllocation::getAllocatedHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal teamAvailable = teamTotal.subtract(teamAllocated);

            long activeReqs = requestRepository.countActiveByAssignedTeam(team.getId());
            BigDecimal estimatedWorkload = requestRepository.sumEstimatedHoursByAssignedTeam(team.getId());

            String utilization;
            if (teamTotal.compareTo(BigDecimal.ZERO) == 0) {
                utilization = "NO_CAPACITY";
            } else {
                double ratio = teamAllocated.doubleValue() / teamTotal.doubleValue();
                if (ratio >= 0.9) utilization = "OVERLOADED";
                else if (ratio >= 0.7) utilization = "HIGH";
                else if (ratio >= 0.4) utilization = "MODERATE";
                else utilization = "LOW";
            }

            teamCapacities.add(CapacityOverviewResponse.TeamCapacity.builder()
                .teamId(team.getId().toString())
                .teamName(team.getName())
                .totalHours(teamTotal)
                .allocatedHours(teamAllocated)
                .availableHours(teamAvailable)
                .activeRequests(activeReqs)
                .estimatedWorkload(estimatedWorkload)
                .utilizationLevel(utilization)
                .build());

            totalAvailable = totalAvailable.add(teamAvailable);
            totalAllocated = totalAllocated.add(teamAllocated);
            totalActiveRequests += activeReqs;
        }

        return CapacityOverviewResponse.builder()
            .teams(teamCapacities)
            .totalAvailableHours(totalAvailable)
            .totalAllocatedHours(totalAllocated)
            .totalActiveRequests(totalActiveRequests)
            .build();
    }
}
