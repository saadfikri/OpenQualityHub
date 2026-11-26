package com.openqualityhub.controller;

import com.openqualityhub.dto.request.CreateDocumentationAssetRequest;
import com.openqualityhub.dto.request.CreateDocumentationReviewRequest;
import com.openqualityhub.dto.request.UpdateReviewStatusRequest;
import com.openqualityhub.dto.response.DocumentationAssetResponse;
import com.openqualityhub.dto.response.DocumentationDashboardResponse;
import com.openqualityhub.service.DocumentationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documentation")
@RequiredArgsConstructor
@Tag(name = "Documentation Intelligence", description = "Documentation asset management, health checks, and review workflows")
public class DocumentationController {

    private final DocumentationService documentationService;

    @PostMapping
    @PreAuthorize("hasAuthority('DOC_WRITE')")
    @Operation(summary = "Register a documentation asset")
    public ResponseEntity<DocumentationAssetResponse> create(@Valid @RequestBody CreateDocumentationAssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentationService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DOC_WRITE')")
    @Operation(summary = "Update a documentation asset")
    public ResponseEntity<DocumentationAssetResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateDocumentationAssetRequest request) {
        return ResponseEntity.ok(documentationService.update(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DOC_READ')")
    @Operation(summary = "Get a documentation asset by ID")
    public ResponseEntity<DocumentationAssetResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(documentationService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DOC_READ')")
    @Operation(summary = "List documentation assets")
    public ResponseEntity<Page<DocumentationAssetResponse>> getAll(
            @RequestParam(required = false) String healthStatus,
            @RequestParam(required = false) String docType,
            @PageableDefault(size = 20) Pageable pageable) {
        if (healthStatus != null) {
            return ResponseEntity.ok(documentationService.getByHealthStatus(healthStatus, pageable));
        }
        if (docType != null) {
            return ResponseEntity.ok(documentationService.getByDocType(docType, pageable));
        }
        return ResponseEntity.ok(documentationService.getAll(pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DOC_WRITE')")
    @Operation(summary = "Soft-delete a documentation asset")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        documentationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/health-checks")
    @PreAuthorize("hasAuthority('DOC_CHECK')")
    @Operation(summary = "Run health checks on a documentation asset")
    public ResponseEntity<DocumentationAssetResponse> runHealthChecks(@PathVariable UUID id) {
        return ResponseEntity.ok(documentationService.runHealthChecks(id));
    }

    @PostMapping("/reviews")
    @PreAuthorize("hasAuthority('DOC_REVIEW')")
    @Operation(summary = "Create a documentation review")
    public ResponseEntity<DocumentationAssetResponse.ReviewResponse> createReview(
            @Valid @RequestBody CreateDocumentationReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentationService.createReview(request));
    }

    @PatchMapping("/reviews/{reviewId}")
    @PreAuthorize("hasAuthority('DOC_REVIEW')")
    @Operation(summary = "Update review status (PENDING, IN_REVIEW, APPROVED, REJECTED, EXPIRED)")
    public ResponseEntity<DocumentationAssetResponse.ReviewResponse> updateReviewStatus(
            @PathVariable UUID reviewId,
            @Valid @RequestBody UpdateReviewStatusRequest request) {
        return ResponseEntity.ok(documentationService.updateReviewStatus(reviewId, request.getStatus(), request.getComment()));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('DOC_READ')")
    @Operation(summary = "Get documentation health dashboard")
    public ResponseEntity<DocumentationDashboardResponse> getDashboard() {
        return ResponseEntity.ok(documentationService.getDashboard());
    }
}
