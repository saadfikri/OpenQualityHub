package com.openqualityhub.service;

import com.openqualityhub.dto.request.CreateQaServiceRequestRequest;
import com.openqualityhub.dto.request.OverridePriorityRequest;
import com.openqualityhub.dto.request.UpdateQaServiceRequestRequest;
import com.openqualityhub.dto.response.QaRequestDashboardResponse;
import com.openqualityhub.dto.response.QaServiceRequestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface QaRequestService {

    QaServiceRequestResponse create(CreateQaServiceRequestRequest request, UUID requestingUserId);

    QaServiceRequestResponse update(UUID id, UpdateQaServiceRequestRequest request, UUID actingUserId);

    QaServiceRequestResponse transition(UUID id, String targetStatus, UUID actingUserId, String comment);

    QaServiceRequestResponse overridePriority(UUID id, OverridePriorityRequest request, UUID actingUserId);

    QaServiceRequestResponse addComment(UUID id, String comment, UUID userId);

    QaServiceRequestResponse getById(UUID id);

    Page<QaServiceRequestResponse> getAll(Pageable pageable);

    Page<QaServiceRequestResponse> getByStatus(String status, Pageable pageable);

    Page<QaServiceRequestResponse> getByTeam(UUID teamId, Pageable pageable);

    Page<QaServiceRequestResponse> getByAssignee(UUID userId, Pageable pageable);

    QaRequestDashboardResponse getDashboard();

    void delete(UUID id);
}
