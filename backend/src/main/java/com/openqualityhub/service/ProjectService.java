package com.openqualityhub.service;

import com.openqualityhub.dto.request.CreateProjectRequest;
import com.openqualityhub.dto.request.UpdateProjectRequest;
import com.openqualityhub.dto.response.ProjectResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProjectService {

    ProjectResponse create(CreateProjectRequest request);

    ProjectResponse getById(UUID id);

    Page<ProjectResponse> getAll(Pageable pageable);

    Page<ProjectResponse> getByTeamId(UUID teamId, Pageable pageable);

    ProjectResponse update(UUID id, UpdateProjectRequest request);

    void delete(UUID id);
}
