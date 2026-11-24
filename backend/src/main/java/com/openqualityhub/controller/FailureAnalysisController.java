package com.openqualityhub.controller;

import com.openqualityhub.dto.request.SubmitFeedbackRequest;
import com.openqualityhub.dto.request.TriggerAnalysisRequest;
import com.openqualityhub.dto.response.FailureAnalysisResponse;
import com.openqualityhub.service.FailureAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/failure-analysis")
@RequiredArgsConstructor
@Tag(name = "Failure Analysis", description = "AI-powered test failure analysis with human feedback")
public class FailureAnalysisController {

    private final FailureAnalysisService failureAnalysisService;

    @PostMapping("/trigger")
    @Operation(summary = "Trigger AI failure analysis for a test run")
    @PreAuthorize("hasAuthority('FAILURE_ANALYSIS_WRITE')")
    public ResponseEntity<FailureAnalysisResponse> trigger(
            @Valid @RequestBody TriggerAnalysisRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(failureAnalysisService.triggerAnalysis(request.getTestRunId()));
    }

    @GetMapping("/test-run/{testRunId}")
    @Operation(summary = "Get failure analysis for a test run")
    @PreAuthorize("hasAuthority('FAILURE_ANALYSIS_READ')")
    public ResponseEntity<FailureAnalysisResponse> getByTestRun(@PathVariable UUID testRunId) {
        return ResponseEntity.ok(failureAnalysisService.getByTestRunId(testRunId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get failure analysis by ID")
    @PreAuthorize("hasAuthority('FAILURE_ANALYSIS_READ')")
    public ResponseEntity<FailureAnalysisResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(failureAnalysisService.getById(id));
    }

    @PostMapping("/{id}/feedback")
    @Operation(summary = "Submit human feedback on AI analysis")
    @PreAuthorize("hasAuthority('FAILURE_ANALYSIS_WRITE')")
    public ResponseEntity<FailureAnalysisResponse> submitFeedback(
            @PathVariable UUID id,
            @Valid @RequestBody SubmitFeedbackRequest request) {
        return ResponseEntity.ok(failureAnalysisService.submitFeedback(id, request));
    }

    @GetMapping("/status")
    @Operation(summary = "Check if AI analysis is available (API key configured)")
    @PreAuthorize("hasAuthority('FAILURE_ANALYSIS_READ')")
    public ResponseEntity<Map<String, Boolean>> getStatus() {
        return ResponseEntity.ok(Map.of("available", failureAnalysisService.isAvailable()));
    }
}
