package com.openqualityhub.controller;

import com.openqualityhub.dto.request.CreateSignOffRequest;
import com.openqualityhub.dto.response.ReadinessReportResponse;
import com.openqualityhub.dto.response.ReleaseDashboardResponse;
import com.openqualityhub.dto.response.SignOffResponse;
import com.openqualityhub.security.UserPrincipal;
import com.openqualityhub.service.ReleaseConfidenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/releases")
@RequiredArgsConstructor
@Tag(name = "Release Confidence", description = "Release readiness reports and sign-off workflow")
public class ReleaseConfidenceController {

    private final ReleaseConfidenceService releaseConfidenceService;

    @PostMapping("/{id}/readiness")
    @Operation(summary = "Generate a readiness report for a release")
    public ResponseEntity<ReadinessReportResponse> generateReadinessReport(
            @PathVariable UUID id, Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(releaseConfidenceService.generateReadinessReport(id, userId));
    }

    @GetMapping("/{id}/readiness")
    @Operation(summary = "Get the latest readiness report for a release")
    public ResponseEntity<ReadinessReportResponse> getLatestReadinessReport(@PathVariable UUID id) {
        return ResponseEntity.ok(releaseConfidenceService.getLatestReadinessReport(id));
    }

    @GetMapping("/{id}/readiness/history")
    @Operation(summary = "Get readiness report history for a release")
    public ResponseEntity<Page<ReadinessReportResponse>> getReadinessHistory(
            @PathVariable UUID id,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(releaseConfidenceService.getReadinessReportHistory(id, pageable));
    }

    @PostMapping("/{id}/sign-offs")
    @Operation(summary = "Sign off on a release (approve or reject)")
    public ResponseEntity<SignOffResponse> signOff(
            @PathVariable UUID id,
            @Valid @RequestBody CreateSignOffRequest request,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(releaseConfidenceService.signOff(id, request, userId));
    }

    @GetMapping("/{id}/sign-offs")
    @Operation(summary = "Get all sign-offs for a release")
    public ResponseEntity<List<SignOffResponse>> getSignOffs(@PathVariable UUID id) {
        return ResponseEntity.ok(releaseConfidenceService.getSignOffs(id));
    }

    @GetMapping("/dashboard/readiness")
    @Operation(summary = "Get release readiness dashboard across all active releases")
    public ResponseEntity<ReleaseDashboardResponse> getReleaseDashboard() {
        return ResponseEntity.ok(releaseConfidenceService.getReleaseDashboard());
    }

    private UUID extractUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal up) {
            return up.getId();
        }
        return null;
    }
}
