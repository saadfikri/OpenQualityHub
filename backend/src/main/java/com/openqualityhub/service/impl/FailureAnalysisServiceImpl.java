package com.openqualityhub.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openqualityhub.dto.request.SubmitFeedbackRequest;
import com.openqualityhub.dto.response.FailureAnalysisResponse;
import com.openqualityhub.entity.FailureAnalysis;
import com.openqualityhub.entity.TestCase;
import com.openqualityhub.entity.TestRun;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.mapper.FailureAnalysisMapper;
import com.openqualityhub.repository.FailureAnalysisRepository;
import com.openqualityhub.repository.TestCaseRepository;
import com.openqualityhub.repository.TestRunRepository;
import com.openqualityhub.security.UserPrincipal;
import com.openqualityhub.service.FailureAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FailureAnalysisServiceImpl implements FailureAnalysisService {

    private static final String SYSTEM_PROMPT = """
            You are an expert test failure analyst for a Quality Engineering platform.
            Analyze the provided test run failure data and determine the root cause.

            You MUST respond with ONLY a valid JSON object (no markdown, no code fences, no explanation outside the JSON).
            Use this exact structure:

            {
              "failure_category": "CODE_REGRESSION|FLAKY_TEST|ENVIRONMENT_ISSUE|DATA_ISSUE|CONFIGURATION_ERROR|DEPENDENCY_FAILURE|TIMEOUT|ASSERTION_MISMATCH|UNKNOWN",
              "summary": "Brief one-sentence summary of the failure",
              "root_cause": "Detailed explanation of the probable root cause",
              "suggested_fix": "Suggested steps to fix the issue",
              "confidence_score": 0.85
            }

            Rules:
            - failure_category must be exactly one of the values listed above
            - confidence_score must be a number between 0.0 and 1.0
            - summary should be concise (one sentence)
            - root_cause should be detailed and technical
            - suggested_fix should contain actionable steps
            - If you cannot determine the category with confidence, use UNKNOWN
            """;

    private static final int MAX_TRACE_LENGTH = 500;

    private final FailureAnalysisRepository failureAnalysisRepository;
    private final TestRunRepository testRunRepository;
    private final TestCaseRepository testCaseRepository;
    private final AnthropicClient anthropicClient;
    private final FailureAnalysisMapper mapper;
    private final ObjectMapper objectMapper;

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

        // Launch async AI processing
        executeAnalysisAsync(analysis.getId(), testRunId);

        return mapper.toResponse(analysis);
    }

    @Async("aiAnalysisExecutor")
    @Transactional
    public void executeAnalysisAsync(UUID analysisId, UUID testRunId) {
        FailureAnalysis analysis = failureAnalysisRepository.findByIdAndDeletedFalse(analysisId)
                .orElse(null);
        if (analysis == null) {
            log.warn("Analysis {} not found when starting async processing", analysisId);
            return;
        }

        TestRun testRun = testRunRepository.findByIdAndDeletedFalse(testRunId)
                .orElse(null);
        if (testRun == null) {
            log.warn("TestRun {} not found when starting async processing", testRunId);
            analysis.setStatus(FailureAnalysis.AnalysisStatus.FAILED);
            analysis.setSummary("Analysis failed: TestRun not found");
            failureAnalysisRepository.save(analysis);
            return;
        }

        try {
            // Build the user prompt from failed test cases
            String userPrompt = buildUserPrompt(testRun);

            // Call Anthropic API (this is the slow part — 5-15s)
            AnthropicClient.AnalysisResult result = anthropicClient.analyze(SYSTEM_PROMPT, userPrompt);

            // Parse the response
            parseAndApplyResult(analysis, result);

            analysis.setStatus(FailureAnalysis.AnalysisStatus.COMPLETED);
            failureAnalysisRepository.save(analysis);
            log.info("AI analysis completed for test run {} (category: {}, confidence: {})",
                    testRunId, analysis.getFailureCategory(), analysis.getConfidenceScore());

        } catch (Exception e) {
            log.error("AI analysis failed for test run {}: {}", testRunId, e.getMessage(), e);
            analysis.setStatus(FailureAnalysis.AnalysisStatus.FAILED);
            analysis.setSummary("Analysis failed: " + e.getMessage());
            failureAnalysisRepository.save(analysis);
        }
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

    private String buildUserPrompt(TestRun testRun) {
        var failedCases = testCaseRepository.findByTestRunIdAndStatus(
                testRun.getId(), TestCase.TestStatus.FAILED, PageRequest.of(0, 50));

        StringBuilder prompt = new StringBuilder();
        prompt.append("## Test Run: ").append(testRun.getName()).append("\n");
        prompt.append("- Source: ").append(testRun.getSource()).append("\n");
        prompt.append("- Total Tests: ").append(testRun.getTotalTests()).append("\n");
        prompt.append("- Passed: ").append(testRun.getPassed()).append("\n");
        prompt.append("- Failed: ").append(testRun.getFailed()).append("\n");
        prompt.append("- Skipped: ").append(testRun.getSkipped()).append("\n");

        if (testRun.getEnvironment() != null) {
            prompt.append("- Environment: ").append(testRun.getEnvironment()).append("\n");
        }
        if (testRun.getBranch() != null) {
            prompt.append("- Branch: ").append(testRun.getBranch()).append("\n");
        }
        if (testRun.getCommitSha() != null) {
            prompt.append("- Commit: ").append(testRun.getCommitSha()).append("\n");
        }

        prompt.append("\n## Failed Test Cases (").append(failedCases.getTotalElements()).append(" total):\n\n");

        for (TestCase tc : failedCases.getContent()) {
            prompt.append("### ").append(tc.getName()).append("\n");
            if (tc.getClassName() != null) {
                prompt.append("- Class: ").append(tc.getClassName()).append("\n");
            }
            if (tc.getErrorMessage() != null) {
                prompt.append("- Error: ").append(truncate(tc.getErrorMessage(), MAX_TRACE_LENGTH)).append("\n");
            }
            if (tc.getStackTrace() != null) {
                prompt.append("- Stack Trace:\n```\n").append(truncate(tc.getStackTrace(), MAX_TRACE_LENGTH)).append("\n```\n");
            }
            prompt.append("\n");
        }

        return prompt.toString();
    }

    private void parseAndApplyResult(FailureAnalysis analysis, AnthropicClient.AnalysisResult result) {
        analysis.setAiModel(result.model());
        analysis.setPromptTokens(result.promptTokens());
        analysis.setCompletionTokens(result.completionTokens());

        try {
            String content = result.content().trim();
            if (content.startsWith("```json")) {
                content = content.substring(7);
            } else if (content.startsWith("```")) {
                content = content.substring(3);
            }
            if (content.endsWith("```")) {
                content = content.substring(0, content.length() - 3);
            }
            content = content.trim();

            JsonNode json = objectMapper.readTree(content);

            String categoryStr = json.path("failure_category").asText(null);
            if (categoryStr != null) {
                try {
                    analysis.setFailureCategory(FailureAnalysis.FailureCategory.valueOf(categoryStr));
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown failure category from AI: {}", categoryStr);
                    analysis.setFailureCategory(FailureAnalysis.FailureCategory.UNKNOWN);
                }
            }

            analysis.setSummary(json.path("summary").asText(null));
            analysis.setRootCause(json.path("root_cause").asText(null));
            analysis.setSuggestedFix(json.path("suggested_fix").asText(null));

            double confidence = json.path("confidence_score").asDouble(-1);
            if (confidence >= 0 && confidence <= 1.0) {
                analysis.setConfidenceScore(BigDecimal.valueOf(confidence));
            }

            analysis.setAnalysisData(content);

        } catch (Exception e) {
            log.warn("Failed to parse AI response as structured JSON, storing raw content: {}", e.getMessage());
            analysis.setSummary("AI response received but could not be parsed as structured JSON.");
            analysis.setFailureCategory(FailureAnalysis.FailureCategory.UNKNOWN);
            analysis.setAnalysisData(result.content());
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "... (truncated)";
    }

    private String resolveCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getEmail();
        }
        return "unknown";
    }
}
