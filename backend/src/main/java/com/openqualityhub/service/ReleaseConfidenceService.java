package com.openqualityhub.service;

import com.openqualityhub.dto.request.CreateSignOffRequest;
import com.openqualityhub.dto.response.ReadinessReportResponse;
import com.openqualityhub.dto.response.ReleaseDashboardResponse;
import com.openqualityhub.dto.response.SignOffResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ReleaseConfidenceService {

    ReadinessReportResponse generateReadinessReport(UUID releaseId, UUID generatedById);

    ReadinessReportResponse getLatestReadinessReport(UUID releaseId);

    Page<ReadinessReportResponse> getReadinessReportHistory(UUID releaseId, Pageable pageable);

    SignOffResponse signOff(UUID releaseId, CreateSignOffRequest request, UUID userId);

    List<SignOffResponse> getSignOffs(UUID releaseId);

    ReleaseDashboardResponse getReleaseDashboard();
}
