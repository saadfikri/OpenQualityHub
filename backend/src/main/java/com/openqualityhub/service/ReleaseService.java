package com.openqualityhub.service;

import com.openqualityhub.dto.response.ReleaseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReleaseService {
    ReleaseResponse getById(UUID id);
    Page<ReleaseResponse> getByProjectId(UUID projectId, Pageable pageable);
    Page<ReleaseResponse> getAll(Pageable pageable);
}
