package com.openqualityhub.service;

import com.openqualityhub.dto.request.CreateDefectRequest;
import com.openqualityhub.dto.request.UpdateDefectRequest;
import com.openqualityhub.dto.response.DefectResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DefectService {
    DefectResponse create(CreateDefectRequest request, UUID reportedById);
    DefectResponse update(UUID id, UpdateDefectRequest request);
    DefectResponse getById(UUID id);
    Page<DefectResponse> getByProjectId(UUID projectId, Pageable pageable);
    Page<DefectResponse> getBySeverity(String severity, Pageable pageable);
    Page<DefectResponse> getByStatus(String status, Pageable pageable);
    Page<DefectResponse> getAll(Pageable pageable);
    void delete(UUID id);
}
