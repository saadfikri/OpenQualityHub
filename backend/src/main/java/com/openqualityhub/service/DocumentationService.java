package com.openqualityhub.service;

import com.openqualityhub.dto.request.CreateDocumentationAssetRequest;
import com.openqualityhub.dto.request.CreateDocumentationReviewRequest;
import com.openqualityhub.dto.response.DocumentationAssetResponse;
import com.openqualityhub.dto.response.DocumentationDashboardResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DocumentationService {

    DocumentationAssetResponse create(CreateDocumentationAssetRequest request);

    DocumentationAssetResponse update(UUID id, CreateDocumentationAssetRequest request);

    DocumentationAssetResponse getById(UUID id);

    Page<DocumentationAssetResponse> getAll(Pageable pageable);

    Page<DocumentationAssetResponse> getByHealthStatus(String healthStatus, Pageable pageable);

    Page<DocumentationAssetResponse> getByDocType(String docType, Pageable pageable);

    void delete(UUID id);

    DocumentationAssetResponse runHealthChecks(UUID assetId);

    DocumentationAssetResponse.ReviewResponse createReview(CreateDocumentationReviewRequest request);

    DocumentationAssetResponse.ReviewResponse updateReviewStatus(UUID reviewId, String status, String comment);

    DocumentationDashboardResponse getDashboard();
}
