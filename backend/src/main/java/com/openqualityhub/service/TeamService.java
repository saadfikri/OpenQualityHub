package com.openqualityhub.service;

import com.openqualityhub.dto.request.CreateTeamRequest;
import com.openqualityhub.dto.request.UpdateTeamRequest;
import com.openqualityhub.dto.response.TeamResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TeamService {

    TeamResponse create(CreateTeamRequest request);

    TeamResponse getById(UUID id);

    Page<TeamResponse> getAll(Pageable pageable);

    Page<TeamResponse> search(String name, Pageable pageable);

    TeamResponse update(UUID id, UpdateTeamRequest request);

    void delete(UUID id);
}
