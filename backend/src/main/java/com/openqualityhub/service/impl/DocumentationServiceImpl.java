package com.openqualityhub.service.impl;

import com.openqualityhub.dto.request.CreateDocumentationAssetRequest;
import com.openqualityhub.dto.request.CreateDocumentationReviewRequest;
import com.openqualityhub.dto.response.DocumentationAssetResponse;
import com.openqualityhub.dto.response.DocumentationDashboardResponse;
import com.openqualityhub.entity.DocumentationAsset;
import com.openqualityhub.entity.DocumentationCheck;
import com.openqualityhub.entity.DocumentationReview;
import com.openqualityhub.entity.Organization;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.mapper.DocumentationAssetMapper;
import com.openqualityhub.repository.*;
import com.openqualityhub.service.DocumentationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentationServiceImpl implements DocumentationService {

    private static final UUID DEFAULT_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final DocumentationAssetRepository assetRepository;
    private final DocumentationCheckRepository checkRepository;
    private final DocumentationReviewRepository reviewRepository;
    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final DocumentationAssetMapper mapper;

    @Override
    @Transactional
    public DocumentationAssetResponse create(CreateDocumentationAssetRequest request) {
        if ((request.getUrl() == null || request.getUrl().isBlank())
                && (request.getRepoPath() == null || request.getRepoPath().isBlank())) {
            throw new IllegalArgumentException("Either 'url' or 'repoPath' must be provided");
        }

        Organization org = organizationRepository.findById(DEFAULT_ORG_ID).orElseThrow();

        DocumentationAsset asset = DocumentationAsset.builder()
                .organization(org)
                .title(request.getTitle())
                .description(request.getDescription())
                .docType(DocumentationAsset.DocType.valueOf(request.getDocType()))
                .url(request.getUrl())
                .repoPath(request.getRepoPath())
                .category(DocumentationAsset.Category.valueOf(request.getCategory()))
                .healthStatus(DocumentationAsset.HealthStatus.UNKNOWN)
                .reviewCycleDays(request.getReviewCycleDays() != null ? request.getReviewCycleDays() : 90)
                .build();

        if (request.getProjectId() != null) {
            projectRepository.findByIdAndDeletedFalse(request.getProjectId()).ifPresent(asset::setProject);
        }
        if (request.getOwnerUserId() != null) {
            userRepository.findById(request.getOwnerUserId()).ifPresent(asset::setOwner);
        }

        asset = assetRepository.save(asset);
        log.info("Created documentation asset: {} ({})", asset.getTitle(), asset.getId());
        return mapper.toResponse(asset);
    }

    @Override
    @Transactional
    public DocumentationAssetResponse update(UUID id, CreateDocumentationAssetRequest request) {
        DocumentationAsset asset = assetRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentationAsset", id));

        if (request.getTitle() != null) asset.setTitle(request.getTitle());
        if (request.getDescription() != null) asset.setDescription(request.getDescription());
        if (request.getDocType() != null) asset.setDocType(DocumentationAsset.DocType.valueOf(request.getDocType()));
        if (request.getUrl() != null) asset.setUrl(request.getUrl());
        if (request.getRepoPath() != null) asset.setRepoPath(request.getRepoPath());
        if (request.getCategory() != null) asset.setCategory(DocumentationAsset.Category.valueOf(request.getCategory()));
        if (request.getReviewCycleDays() != null) asset.setReviewCycleDays(request.getReviewCycleDays());
        if (request.getOwnerUserId() != null) {
            userRepository.findById(request.getOwnerUserId()).ifPresent(asset::setOwner);
        }
        if (request.getProjectId() != null) {
            projectRepository.findByIdAndDeletedFalse(request.getProjectId()).ifPresent(asset::setProject);
        }

        asset = assetRepository.save(asset);
        return mapper.toResponse(asset);
    }

    @Override
    public DocumentationAssetResponse getById(UUID id) {
        return mapper.toResponse(assetRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentationAsset", id)));
    }

    @Override
    public Page<DocumentationAssetResponse> getAll(Pageable pageable) {
        return assetRepository.findByDeletedFalse(pageable).map(mapper::toResponse);
    }

    @Override
    public Page<DocumentationAssetResponse> getByHealthStatus(String healthStatus, Pageable pageable) {
        DocumentationAsset.HealthStatus status = DocumentationAsset.HealthStatus.valueOf(healthStatus);
        return assetRepository.findByHealthStatusAndDeletedFalse(status, pageable).map(mapper::toResponse);
    }

    @Override
    public Page<DocumentationAssetResponse> getByDocType(String docType, Pageable pageable) {
        DocumentationAsset.DocType type = DocumentationAsset.DocType.valueOf(docType);
        return assetRepository.findByDocTypeAndDeletedFalse(type, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        DocumentationAsset asset = assetRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentationAsset", id));
        asset.setDeleted(true);
        assetRepository.save(asset);
        log.info("Soft-deleted documentation asset: {}", id);
    }

    @Override
    @Transactional
    public DocumentationAssetResponse runHealthChecks(UUID assetId) {
        DocumentationAsset asset = assetRepository.findByIdAndDeletedFalse(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentationAsset", assetId));

        List<DocumentationCheck> newChecks = new ArrayList<>();

        // Check 1: Broken link detection
        if (asset.getUrl() != null && !asset.getUrl().isBlank()) {
            newChecks.add(checkBrokenLink(asset));
        }

        // Check 2: Missing ownership
        newChecks.add(checkMissingOwner(asset));

        // Check 3: Stale review detection
        newChecks.add(checkStaleReview(asset));

        // Check 4: Content freshness
        newChecks.add(checkContentFreshness(asset));

        checkRepository.saveAll(newChecks);
        asset.getChecks().addAll(0, newChecks);

        // Compute aggregate health status
        DocumentationAsset.HealthStatus newStatus = computeHealthStatus(newChecks);
        asset.setHealthStatus(newStatus);
        asset.setLastCheckedAt(Instant.now());
        asset = assetRepository.save(asset);

        log.info("Ran health checks on asset {} -> {}", assetId, newStatus);
        return mapper.toResponse(asset);
    }

    @Override
    @Transactional
    public DocumentationAssetResponse.ReviewResponse createReview(CreateDocumentationReviewRequest request) {
        DocumentationAsset asset = assetRepository.findByIdAndDeletedFalse(request.getAssetId())
                .orElseThrow(() -> new ResourceNotFoundException("DocumentationAsset", request.getAssetId()));

        DocumentationReview review = DocumentationReview.builder()
                .asset(asset)
                .status(DocumentationReview.ReviewStatus.PENDING)
                .dueDate(request.getDueDate())
                .build();

        if (request.getReviewerId() != null) {
            userRepository.findById(request.getReviewerId()).ifPresent(review::setReviewer);
        }

        review = reviewRepository.save(review);
        log.info("Created documentation review {} for asset {}", review.getId(), asset.getId());
        return mapper.toReviewResponse(review);
    }

    @Override
    @Transactional
    public DocumentationAssetResponse.ReviewResponse updateReviewStatus(UUID reviewId, String status, String comment) {
        DocumentationReview review = reviewRepository.findByIdAndDeletedFalse(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentationReview", reviewId));

        DocumentationReview.ReviewStatus newStatus = DocumentationReview.ReviewStatus.valueOf(status);
        review.setStatus(newStatus);
        if (comment != null) review.setComment(comment);

        if (newStatus == DocumentationReview.ReviewStatus.APPROVED || newStatus == DocumentationReview.ReviewStatus.REJECTED) {
            review.setCompletedAt(Instant.now());
            if (newStatus == DocumentationReview.ReviewStatus.APPROVED) {
                DocumentationAsset asset = review.getAsset();
                asset.setLastReviewedAt(Instant.now());
                assetRepository.save(asset);
            }
        }

        review = reviewRepository.save(review);
        log.info("Updated review {} status to {}", reviewId, newStatus);
        return mapper.toReviewResponse(review);
    }

    @Override
    public DocumentationDashboardResponse getDashboard() {
        long total = assetRepository.countByDeletedFalse();
        long healthy = assetRepository.countByHealthStatusAndDeletedFalse(DocumentationAsset.HealthStatus.HEALTHY);
        long warning = assetRepository.countByHealthStatusAndDeletedFalse(DocumentationAsset.HealthStatus.WARNING);
        long critical = assetRepository.countByHealthStatusAndDeletedFalse(DocumentationAsset.HealthStatus.CRITICAL);
        long unknown = assetRepository.countByHealthStatusAndDeletedFalse(DocumentationAsset.HealthStatus.UNKNOWN);
        long missingOwner = assetRepository.countByOwnerIsNullAndDeletedFalse();
        long pendingReviews = reviewRepository.countByStatusAndDeletedFalse(DocumentationReview.ReviewStatus.PENDING);

        List<DocumentationDashboardResponse.TypeCount> byType = List.of(
                DocumentationAsset.DocType.values()).stream()
                .map(type -> {
                    long count = assetRepository.findByDocTypeAndDeletedFalse(type, Pageable.unpaged()).getTotalElements();
                    return DocumentationDashboardResponse.TypeCount.builder()
                            .type(type.name())
                            .count(count)
                            .build();
                })
                .filter(tc -> tc.getCount() > 0)
                .collect(Collectors.toList());

        return DocumentationDashboardResponse.builder()
                .totalAssets(total)
                .healthyCount(healthy)
                .warningCount(warning)
                .criticalCount(critical)
                .unknownCount(unknown)
                .missingOwnerCount(missingOwner)
                .pendingReviews(pendingReviews)
                .assetsByType(byType)
                .build();
    }

    // --- Health check logic ---

    private DocumentationCheck checkBrokenLink(DocumentationAsset asset) {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(asset.getUrl()).toURL().openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            conn.disconnect();

            if (code >= 200 && code < 400) {
                return buildCheck(asset, DocumentationCheck.CheckType.BROKEN_LINK,
                        DocumentationCheck.CheckStatus.PASS, "URL reachable (HTTP " + code + ")");
            } else {
                return buildCheck(asset, DocumentationCheck.CheckType.BROKEN_LINK,
                        DocumentationCheck.CheckStatus.FAIL, "URL returned HTTP " + code);
            }
        } catch (Exception e) {
            return buildCheck(asset, DocumentationCheck.CheckType.BROKEN_LINK,
                    DocumentationCheck.CheckStatus.FAIL, "URL unreachable: " + e.getMessage());
        }
    }

    private DocumentationCheck checkMissingOwner(DocumentationAsset asset) {
        if (asset.getOwner() != null) {
            return buildCheck(asset, DocumentationCheck.CheckType.MISSING_OWNER,
                    DocumentationCheck.CheckStatus.PASS, "Owner assigned: " + asset.getOwner().getDisplayName());
        }
        return buildCheck(asset, DocumentationCheck.CheckType.MISSING_OWNER,
                DocumentationCheck.CheckStatus.FAIL, "No owner assigned to this documentation asset");
    }

    private DocumentationCheck checkStaleReview(DocumentationAsset asset) {
        if (asset.getLastReviewedAt() == null) {
            return buildCheck(asset, DocumentationCheck.CheckType.STALE_REVIEW,
                    DocumentationCheck.CheckStatus.WARNING, "Never reviewed");
        }
        long daysSinceReview = ChronoUnit.DAYS.between(asset.getLastReviewedAt(), Instant.now());
        if (daysSinceReview > asset.getReviewCycleDays()) {
            return buildCheck(asset, DocumentationCheck.CheckType.STALE_REVIEW,
                    DocumentationCheck.CheckStatus.FAIL,
                    "Last reviewed " + daysSinceReview + " days ago (cycle: " + asset.getReviewCycleDays() + " days)");
        }
        if (daysSinceReview > asset.getReviewCycleDays() * 0.8) {
            return buildCheck(asset, DocumentationCheck.CheckType.STALE_REVIEW,
                    DocumentationCheck.CheckStatus.WARNING,
                    "Review due soon: " + daysSinceReview + " days since last review");
        }
        return buildCheck(asset, DocumentationCheck.CheckType.STALE_REVIEW,
                DocumentationCheck.CheckStatus.PASS,
                "Reviewed " + daysSinceReview + " days ago (within " + asset.getReviewCycleDays() + "-day cycle)");
    }

    private DocumentationCheck checkContentFreshness(DocumentationAsset asset) {
        long daysSinceUpdate = ChronoUnit.DAYS.between(asset.getUpdatedAt(), Instant.now());
        if (daysSinceUpdate > 365) {
            return buildCheck(asset, DocumentationCheck.CheckType.CONTENT_FRESHNESS,
                    DocumentationCheck.CheckStatus.FAIL,
                    "Not updated in " + daysSinceUpdate + " days (>1 year)");
        }
        if (daysSinceUpdate > 180) {
            return buildCheck(asset, DocumentationCheck.CheckType.CONTENT_FRESHNESS,
                    DocumentationCheck.CheckStatus.WARNING,
                    "Not updated in " + daysSinceUpdate + " days (>6 months)");
        }
        return buildCheck(asset, DocumentationCheck.CheckType.CONTENT_FRESHNESS,
                DocumentationCheck.CheckStatus.PASS,
                "Updated " + daysSinceUpdate + " days ago");
    }

    private DocumentationCheck buildCheck(DocumentationAsset asset, DocumentationCheck.CheckType type,
                                          DocumentationCheck.CheckStatus status, String message) {
        return DocumentationCheck.builder()
                .asset(asset)
                .checkType(type)
                .status(status)
                .message(message)
                .executedAt(Instant.now())
                .build();
    }

    private DocumentationAsset.HealthStatus computeHealthStatus(List<DocumentationCheck> checks) {
        boolean hasFail = checks.stream().anyMatch(c -> c.getStatus() == DocumentationCheck.CheckStatus.FAIL);
        boolean hasWarning = checks.stream().anyMatch(c -> c.getStatus() == DocumentationCheck.CheckStatus.WARNING);

        if (hasFail) return DocumentationAsset.HealthStatus.CRITICAL;
        if (hasWarning) return DocumentationAsset.HealthStatus.WARNING;
        return DocumentationAsset.HealthStatus.HEALTHY;
    }
}
