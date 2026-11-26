package com.openqualityhub.service.impl;

import com.openqualityhub.dto.request.SubmitFeedbackRequest;
import com.openqualityhub.dto.response.FailureAnalysisResponse;
import com.openqualityhub.entity.FailureAnalysis;
import com.openqualityhub.entity.TestRun;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.mapper.FailureAnalysisMapper;
import com.openqualityhub.repository.FailureAnalysisRepository;
import com.openqualityhub.repository.TestRunRepository;
import com.openqualityhub.security.UserPrincipal;
import com.openqualityhub.service.FailureAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FailureAnalysisServiceImpl implements FailureAnalysisService {

    private final FailureAnalysisRepository failureAnalysisRepository;
    private final TestRunRepository testRunRepository;
    private final AnthropicClient anthropicClient;
    private final FailureAnalysisMapper mapper;
    private final FailureAnalysisAsyncProcessor asyncProcessor;

    @Override
    @Transactional
    public FailureAnalysisResponse triggerAnalysis(UUID testRunId) {
        TestRun testRun = testRunRepository.findByIdAndDeletedFalse(testRunId)
                .orElseThrow(() -> new ResourceNotFoundException("TestRun", testRunId));

        if (testRun.getFailed() == 0) {
            throw new IllegalArgumentException("Test run has no failures to analyze");
        }

        // Check if analysis already exists
        failureAnalysisRepository.findByTestRunIdAndDeletedFalse(testRunId)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Analysis already exists for this test run (id: " + existing.getId() + "). "
                            + "Delete the existing analysis before triggering a new one.");
                });

        // Create analysis record
        FailureAnalysis analysis = FailureAnalysis.builder()
                .testRun(testRun)
                .status(FailureAnalysis.AnalysisStatus.PENDING)
                .build();

        // If AI is not available, mark as SKIPPED
        if (!anthropicClient.isEnabled()) {
            analysis.setStatus(FailureAnalysis.AnalysisStatus.SKIPPED);
            analysis.setSummary("AI analysis unavailable: ANTHROPIC_API_KEY is not configured.");
            analysis = failureAnalysisRepository.save(analysis);
            log.warn("AI analysis skipped for test run {} - API key not configured", testRunId);
            return mapper.toResponse(analysis);
        }

        // Set to ANALYZING and save — return immediately
        analysis.setStatus(FailureAnalysis.AnalysisStatus.ANALYZING);
        analysis = failureAnalysisRepository.save(analysis);
        log.info("AI analysis triggered for test run {} (analysis id: {})", testRunId, analysis.getId());

        // Launch async AI processing via separate bean (required for @Async proxy)
        asyncProcessor.executeAnalysisAsync(analysis.getId(), testRunId);

        return mapper.toResponse(analysis);
    }

    @Override
    public FailureAnalysisResponse getByTestRunId(UUID testRunId) {
        FailureAnalysis analysis = failureAnalysisRepository.findByTestRunIdAndDeletedFalse(testRunId)
                .orElseThrow(() -> new ResourceNotFoundException("FailureAnalysis for TestRun", testRunId));
        return mapper.toResponse(analysis);
    }

    @Override
    public FailureAnalysisResponse getById(UUID id) {
        FailureAnalysis analysis = failureAnalysisRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("FailureAnalysis", id));
        return mapper.toResponse(analysis);
    }

    @Override
    @Transactional
    public FailureAnalysisResponse submitFeedback(UUID analysisId, SubmitFeedbackRequest request) {
        FailureAnalysis analysis = failureAnalysisRepository.findByIdAndDeletedFalse(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("FailureAnalysis", analysisId));

        if (analysis.getStatus() != FailureAnalysis.AnalysisStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot submit feedback on an analysis that is not completed (current status: " + analysis.getStatus() + ")");
        }

        FailureAnalysis.HumanFeedback feedback;
        try {
            feedback = FailureAnalysis.HumanFeedback.valueOf(request.getFeedback());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid feedback value: " + request.getFeedback()
                    + ". Must be one of: ACCEPTED, REJECTED, PARTIALLY_ACCEPTED");
        }

        if (feedback == FailureAnalysis.HumanFeedback.PENDING) {
            throw new IllegalArgumentException("Cannot set feedback to PENDING. Must be: ACCEPTED, REJECTED, or PARTIALLY_ACCEPTED");
        }

        analysis.setHumanFeedback(feedback);
        analysis.setFeedbackComment(request.getComment());
        analysis.setFeedbackBy(resolveCurrentUserEmail());
        analysis.setFeedbackAt(Instant.now());

        analysis = failureAnalysisRepository.save(analysis);
        log.info("Feedback submitted for analysis {}: {} by {}", analysisId, feedback, analysis.getFeedbackBy());
        return mapper.toResponse(analysis);
    }

    @Override
    public boolean isAvailable() {
        return anthropicClient.isEnabled();
    }

    private String resolveCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getEmail();
        }
        return "unknown";
    }
}
