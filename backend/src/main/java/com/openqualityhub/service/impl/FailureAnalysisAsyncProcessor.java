package com.openqualityhub.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openqualityhub.entity.FailureAnalysis;
import com.openqualityhub.entity.TestRun;
import com.openqualityhub.repository.FailureAnalysisRepository;
import com.openqualityhub.repository.TestRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FailureAnalysisAsyncProcessor {

    private final FailureAnalysisRepository failureAnalysisRepository;
    private final TestRunRepository testRunRepository;
    private final AnthropicClient anthropicClient;
    private final ObjectMapper objectMapper;
    private final FailureAnalysisPromptBuilder promptBuilder;

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
            String userPrompt = promptBuilder.buildUserPrompt(testRun);

            AnthropicClient.AnalysisResult result = anthropicClient.analyze(
                    FailureAnalysisPromptBuilder.SYSTEM_PROMPT, userPrompt);

            parseAndApplyResult(analysis, result);

            analysis.setStatus(FailureAnalysis.AnalysisStatus.COMPLETED);
            failureAnalysisRepository.save(analysis);
            log.info("AI analysis completed for test run {} (category: {}, confidence: {})",
                    testRunId, analysis.getFailureCategory(), analysis.getConfidenceScore());

        } catch (Exception e) {
            log.error("AI analysis failed for test run {}: {}", testRunId, e.getMessage(), e);
            analysis.setStatus(FailureAnalysis.AnalysisStatus.FAILED);
            analysis.setSummary("Analysis failed: " + e.getMessage());
            analysis.setAnalysisData(null);
            failureAnalysisRepository.save(analysis);
        }
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

            // Store re-serialized JSON to guarantee valid jsonb
            analysis.setAnalysisData(objectMapper.writeValueAsString(json));

        } catch (Exception e) {
            log.warn("Failed to parse AI response as structured JSON: {}", e.getMessage());
            analysis.setSummary("AI response received but could not be parsed as structured JSON.");
            analysis.setFailureCategory(FailureAnalysis.FailureCategory.UNKNOWN);
            // Wrap raw content in JSON envelope for safe jsonb storage
            try {
                analysis.setAnalysisData(objectMapper.writeValueAsString(
                        Map.of("raw_content", result.content())));
            } catch (Exception ex) {
                analysis.setAnalysisData("{}");
            }
        }
    }
}
