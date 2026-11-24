package com.openqualityhub.service.impl;

import com.openqualityhub.dto.request.CreateProjectRequest;
import com.openqualityhub.dto.request.UpdateProjectRequest;
import com.openqualityhub.dto.response.ProjectResponse;
import com.openqualityhub.entity.Project;
import com.openqualityhub.entity.Team;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.mapper.ProjectMapper;
import com.openqualityhub.repository.ProjectRepository;
import com.openqualityhub.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private ProjectMapper projectMapper;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private UUID teamId;
    private UUID projectId;
    private Team team;

    @BeforeEach
    void setUp() {
        teamId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        team = Team.builder().name("QA Team").build();
        team.setId(teamId);
    }

    @Test
    void create_shouldLookupTeamViaSoftDeleteAwareQuery() {
        CreateProjectRequest req = new CreateProjectRequest();
        req.setName("API Service");
        req.setTeamId(teamId);

        Project entity = Project.builder().name("API Service").team(team).build();
        ProjectResponse response = new ProjectResponse();

        when(teamRepository.findByIdAndDeletedFalse(teamId)).thenReturn(Optional.of(team));
        when(projectRepository.existsByNameAndTeamIdAndDeletedFalse("API Service", teamId)).thenReturn(false);
        when(projectMapper.toEntity(req)).thenReturn(entity);
        when(projectRepository.save(any())).thenReturn(entity);
        when(projectMapper.toResponse(any())).thenReturn(response);

        projectService.create(req);

        verify(teamRepository).findByIdAndDeletedFalse(teamId);
        verify(teamRepository, never()).findById(any());
    }

    @Test
    void create_shouldThrowWhenTeamNotFound() {
        CreateProjectRequest req = new CreateProjectRequest();
        req.setName("P");
        req.setTeamId(teamId);

        when(teamRepository.findByIdAndDeletedFalse(teamId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.create(req));
    }

    @Test
    void create_shouldThrowOnDuplicateName() {
        CreateProjectRequest req = new CreateProjectRequest();
        req.setName("Existing");
        req.setTeamId(teamId);

        when(teamRepository.findByIdAndDeletedFalse(teamId)).thenReturn(Optional.of(team));
        when(projectRepository.existsByNameAndTeamIdAndDeletedFalse("Existing", teamId)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> projectService.create(req));
    }

    @Test
    void getById_shouldUseSoftDeleteAwareQuery() {
        Project project = Project.builder().name("P").team(team).build();
        project.setId(projectId);

        when(projectRepository.findByIdAndDeletedFalse(projectId)).thenReturn(Optional.of(project));
        when(projectMapper.toResponse(project)).thenReturn(new ProjectResponse());

        projectService.getById(projectId);

        verify(projectRepository).findByIdAndDeletedFalse(projectId);
        verify(projectRepository, never()).findById(any());
    }

    @Test
    void update_shouldUseSoftDeleteAwareQuery() {
        Project project = Project.builder().name("Old").team(team).build();
        project.setId(projectId);
        UpdateProjectRequest req = new UpdateProjectRequest();
        req.setDescription("Updated");

        when(projectRepository.findByIdAndDeletedFalse(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenReturn(project);
        when(projectMapper.toResponse(any())).thenReturn(new ProjectResponse());

        projectService.update(projectId, req);

        verify(projectRepository).findByIdAndDeletedFalse(projectId);
    }

    @Test
    void delete_shouldSoftDelete() {
        Project project = Project.builder().name("ToDelete").team(team).build();
        project.setId(projectId);

        when(projectRepository.findByIdAndDeletedFalse(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenReturn(project);

        projectService.delete(projectId);

        assertTrue(project.isDeleted());
    }

    @Test
    void getByTeamId_shouldVerifyTeamExistsViaSoftDelete() {
        when(teamRepository.findByIdAndDeletedFalse(teamId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> projectService.getByTeamId(teamId, org.springframework.data.domain.Pageable.unpaged()));
    }
}
