package com.openqualityhub.service.impl;

import com.openqualityhub.dto.request.CreateTeamRequest;
import com.openqualityhub.dto.request.UpdateTeamRequest;
import com.openqualityhub.dto.response.TeamResponse;
import com.openqualityhub.entity.Organization;
import com.openqualityhub.entity.Team;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.mapper.TeamMapper;
import com.openqualityhub.repository.OrganizationRepository;
import com.openqualityhub.repository.TeamRepository;
import com.openqualityhub.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamServiceImpl implements TeamService {

    private static final UUID DEFAULT_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final TeamRepository teamRepository;
    private final OrganizationRepository organizationRepository;
    private final TeamMapper teamMapper;

    @Override
    @Transactional
    public TeamResponse create(CreateTeamRequest request) {
        if (teamRepository.existsByNameAndDeletedFalse(request.getName())) {
            throw new IllegalArgumentException("Team with name '" + request.getName() + "' already exists");
        }

        Organization org = organizationRepository.findById(DEFAULT_ORG_ID)
            .orElseThrow(() -> new ResourceNotFoundException("Organization", DEFAULT_ORG_ID));

        Team team = teamMapper.toEntity(request);
        team.setOrganization(org);
        team = teamRepository.save(team);
        log.info("Created team: {} ({})", team.getName(), team.getId());
        return teamMapper.toResponse(team);
    }

    @Override
    public TeamResponse getById(UUID id) {
        Team team = teamRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("Team", id));
        return teamMapper.toResponse(team);
    }

    @Override
    public Page<TeamResponse> getAll(Pageable pageable) {
        return teamRepository.findByDeletedFalse(pageable).map(teamMapper::toResponse);
    }

    @Override
    public Page<TeamResponse> search(String name, Pageable pageable) {
        return teamRepository.findByDeletedFalseAndNameContainingIgnoreCase(name, pageable)
            .map(teamMapper::toResponse);
    }

    @Override
    @Transactional
    public TeamResponse update(UUID id, UpdateTeamRequest request) {
        Team team = teamRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("Team", id));

        if (request.getName() != null && !request.getName().isBlank()) {
            if (!team.getName().equals(request.getName()) && teamRepository.existsByNameAndDeletedFalse(request.getName())) {
                throw new IllegalArgumentException("Team with name '" + request.getName() + "' already exists");
            }
            team.setName(request.getName());
        }

        if (request.getDescription() != null) {
            team.setDescription(request.getDescription());
        }

        team = teamRepository.save(team);
        log.info("Updated team: {} ({})", team.getName(), team.getId());
        return teamMapper.toResponse(team);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Team team = teamRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("Team", id));
        team.setDeleted(true);
        teamRepository.save(team);
        log.info("Soft-deleted team: {}", id);
    }
}
