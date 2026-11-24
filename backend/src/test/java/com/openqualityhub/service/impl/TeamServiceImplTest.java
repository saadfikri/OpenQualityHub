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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceImplTest {

    @Mock private TeamRepository teamRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private TeamMapper teamMapper;

    @InjectMocks
    private TeamServiceImpl teamService;

    private static final UUID DEFAULT_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private Organization org;
    private UUID teamId;

    @BeforeEach
    void setUp() {
        org = Organization.builder().name("Demo").build();
        org.setId(DEFAULT_ORG_ID);
        teamId = UUID.randomUUID();
    }

    @Test
    void create_shouldSaveTeamWithOrganization() {
        CreateTeamRequest req = new CreateTeamRequest();
        req.setName("QA Core");
        req.setDescription("Core QA team");

        Team entity = Team.builder().name("QA Core").build();
        TeamResponse response = new TeamResponse();
        response.setName("QA Core");

        when(teamRepository.existsByNameAndDeletedFalse("QA Core")).thenReturn(false);
        when(organizationRepository.findById(DEFAULT_ORG_ID)).thenReturn(Optional.of(org));
        when(teamMapper.toEntity(req)).thenReturn(entity);
        when(teamRepository.save(any(Team.class))).thenReturn(entity);
        when(teamMapper.toResponse(entity)).thenReturn(response);

        TeamResponse result = teamService.create(req);

        assertEquals("QA Core", result.getName());
        verify(teamRepository).save(entity);
    }

    @Test
    void create_shouldThrowOnDuplicateName() {
        CreateTeamRequest req = new CreateTeamRequest();
        req.setName("Existing");

        when(teamRepository.existsByNameAndDeletedFalse("Existing")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> teamService.create(req));
    }

    @Test
    void getById_shouldUseSoftDeleteAwareQuery() {
        Team team = Team.builder().name("T").build();
        team.setId(teamId);
        TeamResponse response = new TeamResponse();

        when(teamRepository.findByIdAndDeletedFalse(teamId)).thenReturn(Optional.of(team));
        when(teamMapper.toResponse(team)).thenReturn(response);

        teamService.getById(teamId);

        verify(teamRepository).findByIdAndDeletedFalse(teamId);
        verify(teamRepository, never()).findById(any());
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(teamRepository.findByIdAndDeletedFalse(teamId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> teamService.getById(teamId));
    }

    @Test
    void update_shouldUseSoftDeleteAwareQuery() {
        Team team = Team.builder().name("Old").build();
        team.setId(teamId);
        UpdateTeamRequest req = new UpdateTeamRequest();
        req.setDescription("Updated desc");

        when(teamRepository.findByIdAndDeletedFalse(teamId)).thenReturn(Optional.of(team));
        when(teamRepository.save(any())).thenReturn(team);
        when(teamMapper.toResponse(any())).thenReturn(new TeamResponse());

        teamService.update(teamId, req);

        verify(teamRepository).findByIdAndDeletedFalse(teamId);
    }

    @Test
    void update_shouldThrowOnDuplicateRename() {
        Team team = Team.builder().name("Old").build();
        team.setId(teamId);
        UpdateTeamRequest req = new UpdateTeamRequest();
        req.setName("Taken");

        when(teamRepository.findByIdAndDeletedFalse(teamId)).thenReturn(Optional.of(team));
        when(teamRepository.existsByNameAndDeletedFalse("Taken")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> teamService.update(teamId, req));
    }

    @Test
    void delete_shouldSoftDelete() {
        Team team = Team.builder().name("ToDelete").build();
        team.setId(teamId);

        when(teamRepository.findByIdAndDeletedFalse(teamId)).thenReturn(Optional.of(team));
        when(teamRepository.save(any())).thenReturn(team);

        teamService.delete(teamId);

        assertTrue(team.isDeleted());
        verify(teamRepository).save(team);
    }

    @Test
    void delete_shouldThrowWhenNotFound() {
        when(teamRepository.findByIdAndDeletedFalse(teamId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> teamService.delete(teamId));
    }
}
