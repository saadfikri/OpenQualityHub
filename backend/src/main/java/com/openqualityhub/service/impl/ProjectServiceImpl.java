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
import com.openqualityhub.service.ProjectService;
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
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final ProjectMapper projectMapper;

    @Override
    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        Team team = teamRepository.findByIdAndDeletedFalse(request.getTeamId())
            .orElseThrow(() -> new ResourceNotFoundException("Team", request.getTeamId()));

        if (projectRepository.existsByNameAndTeamIdAndDeletedFalse(request.getName(), request.getTeamId())) {
            throw new IllegalArgumentException(
                "Project with name '" + request.getName() + "' already exists in team '" + team.getName() + "'"
            );
        }

        Project project = projectMapper.toEntity(request);
        project.setTeam(team);
        project = projectRepository.save(project);
        log.info("Created project: {} ({}) in team {}", project.getName(), project.getId(), team.getName());
        return projectMapper.toResponse(project);
    }

    @Override
    public ProjectResponse getById(UUID id) {
        Project project = projectRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        return projectMapper.toResponse(project);
    }

    @Override
    public Page<ProjectResponse> getAll(Pageable pageable) {
        return projectRepository.findByDeletedFalse(pageable).map(projectMapper::toResponse);
    }

    @Override
    public Page<ProjectResponse> getByTeamId(UUID teamId, Pageable pageable) {
        if (!teamRepository.findByIdAndDeletedFalse(teamId).isPresent()) {
            throw new ResourceNotFoundException("Team", teamId);
        }
        return projectRepository.findByTeamIdAndDeletedFalse(teamId, pageable).map(projectMapper::toResponse);
    }

    @Override
    @Transactional
    public ProjectResponse update(UUID id, UpdateProjectRequest request) {
        Project project = projectRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project", id));

        if (request.getName() != null && !request.getName().isBlank()) {
            if (!project.getName().equals(request.getName())
                && projectRepository.existsByNameAndTeamIdAndDeletedFalse(request.getName(), project.getTeam().getId())) {
                throw new IllegalArgumentException(
                    "Project with name '" + request.getName() + "' already exists in this team"
                );
            }
            project.setName(request.getName());
        }

        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }

        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }

        project = projectRepository.save(project);
        log.info("Updated project: {} ({})", project.getName(), project.getId());
        return projectMapper.toResponse(project);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Project project = projectRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        project.setDeleted(true);
        projectRepository.save(project);
        log.info("Soft-deleted project: {}", id);
    }
}
