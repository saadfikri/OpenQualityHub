package com.openqualityhub.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openqualityhub.entity.FailureAnalysis;
import com.openqualityhub.entity.TestRun;
import com.openqualityhub.repository.FailureAnalysisRepository;
import com.openqualityhub.repository.TestRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FailureAnalysisAsyncProcessorTest {

    @Mock private FailureAnalysisRepository failureAnalysisRepository;
    @Mock private TestRunRepository testRunRepository;
    @Mock private AnthropicClient anthropicClient;
    @Mock private FailureAnalysisPromptBuilder promptBuilder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private FailureAnalysisAsyncProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new FailureAnalysisAsyncProcessor(
                failureAnalysisRepository, testRunRepository,
                anthropicClient, objectMapper, promptBuilder);
    }

    @Test
    void executeAnalysisAsync_success() throws Exception {
        UUID analysisId = UUID.randomUUID();
        UUID testRunId = UUID.randomUUID();
        TestRun testRun = buildTestRun(testRunId, 3);

        FailureAnalysis analysis = FailureAnalysis.builder()
                .testRun(testRun)
                .status(FailureAnalysis.AnalysisStatus.ANALYZING)
                .build();
        analysis.setId(analysisId);

        when(failureAnalysisRepository.findByIdAndDeletedFalse(analysisId)).thenReturn(Optional.of(analysis));
        when(testRunRepository.findByIdAndDeletedFalse(testRunId)).thenReturn(Optional.of(testRun));
        when(promptBuilder.buildUserPrompt(testRun)).thenReturn("test prompt");

        String aiResponse = """
                {"failure_category":"CODE_REGRESSION","summary":"NPE in login","root_cause":"Null user object","suggested_fix":"Check user initialization","confidence_score":0.9}
                """;
        when(anthropicClient.analyze(any(), any()))
                .thenReturn(new AnthropicClient.AnalysisResult(aiResponse, "claude-sonnet-4-20250514", 100, 200));
        when(failureAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        processor.executeAnalysisAsync(analysisId, testRunId);

        ArgumentCaptor<FailureAnalysis> captor = ArgumentCaptor.forClass(FailureAnalysis.class);
        verify(failureAnalysisRepository).save(captor.capture());
        FailureAnalysis saved = captor.getValue();
        assertEquals(FailureAnalysis.AnalysisStatus.COMPLETED, saved.getStatus());
        assertEquals(FailureAnalysis.FailureCategory.CODE_REGRESSION, saved.getFailureCategory());
        assertEquals("NPE in login", saved.getSummary());
        assertEquals("claude-sonnet-4-20250514", saved.getAiModel());
        assertEquals(100, saved.getPromptTokens());
        assertEquals(200, saved.getCompletionTokens());
    }

    @Test
    void executeAnalysisAsync_aiError_statusFailed() throws Exception {
        UUID analysisId = UUID.randomUUID();
        UUID testRunId = UUID.randomUUID();
        TestRun testRun = buildTestRun(testRunId, 1);

        FailureAnalysis analysis = FailureAnalysis.builder()
                .testRun(testRun)
                .status(FailureAnalysis.AnalysisStatus.ANALYZING)
                .build();
        analysis.setId(analysisId);

        when(failureAnalysisRepository.findByIdAndDeletedFalse(analysisId)).thenReturn(Optional.of(analysis));
        when(testRunRepository.findByIdAndDeletedFalse(testRunId)).thenReturn(Optional.of(testRun));
        when(promptBuilder.buildUserPrompt(testRun)).thenReturn("test prompt");
        when(anthropicClient.analyze(any(), any())).thenThrow(new RuntimeException("API timeout"));
        when(failureAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        processor.executeAnalysisAsync(analysisId, testRunId);

        ArgumentCaptor<FailureAnalysis> captor = ArgumentCaptor.forClass(FailureAnalysis.class);
        verify(failureAnalysisRepository).save(captor.capture());
        assertEquals(FailureAnalysis.AnalysisStatus.FAILED, captor.getValue().getStatus());
        assertTrue(captor.getValue().getSummary().contains("API timeout"));
    }

    @Test
    void executeAnalysisAsync_malformedAiResponse_setsUnknown() throws Exception {
        UUID analysisId = UUID.randomUUID();
        UUID testRunId = UUID.randomUUID();
        TestRun testRun = buildTestRun(testRunId, 1);

        FailureAnalysis analysis = FailureAnalysis.builder()
                .testRun(testRun)
                .status(FailureAnalysis.AnalysisStatus.ANALYZING)
                .build();
        analysis.setId(analysisId);

        when(failureAnalysisRepository.findByIdAndDeletedFalse(analysisId)).thenReturn(Optional.of(analysis));
        when(testRunRepository.findByIdAndDeletedFalse(testRunId)).thenReturn(Optional.of(testRun));
        when(promptBuilder.buildUserPrompt(testRun)).thenReturn("test prompt");
        when(anthropicClient.analyze(any(), any()))
                .thenReturn(new AnthropicClient.AnalysisResult("not valid json at all", "claude-sonnet-4-20250514", 50, 30));
        when(failureAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        processor.executeAnalysisAsync(analysisId, testRunId);

        ArgumentCaptor<FailureAnalysis> captor = ArgumentCaptor.forClass(FailureAnalysis.class);
        verify(failureAnalysisRepository).save(captor.capture());
        FailureAnalysis last = captor.getValue();
        assertEquals(FailureAnalysis.AnalysisStatus.COMPLETED, last.getStatus());
        assertEquals(FailureAnalysis.FailureCategory.UNKNOWN, last.getFailureCategory());
        assertNotNull(last.getAnalysisData());
    }

    @Test
    void executeAnalysisAsync_analysisNotFound_returns() {
        UUID analysisId = UUID.randomUUID();
        UUID testRunId = UUID.randomUUID();

        when(failureAnalysisRepository.findByIdAndDeletedFalse(analysisId)).thenReturn(Optional.empty());

        processor.executeAnalysisAsync(analysisId, testRunId);

        verify(failureAnalysisRepository, never()).save(any());
    }

    @Test
    void executeAnalysisAsync_testRunNotFound_marksFailed() {
        UUID analysisId = UUID.randomUUID();
        UUID testRunId = UUID.randomUUID();

        FailureAnalysis analysis = FailureAnalysis.builder()
                .status(FailureAnalysis.AnalysisStatus.ANALYZING)
                .build();
        analysis.setId(analysisId);

        when(failureAnalysisRepository.findByIdAndDeletedFalse(analysisId)).thenReturn(Optional.of(analysis));
        when(testRunRepository.findByIdAndDeletedFalse(testRunId)).thenReturn(Optional.empty());
        when(failureAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        processor.executeAnalysisAsync(analysisId, testRunId);

        ArgumentCaptor<FailureAnalysis> captor = ArgumentCaptor.forClass(FailureAnalysis.class);
        verify(failureAnalysisRepository).save(captor.capture());
        assertEquals(FailureAnalysis.AnalysisStatus.FAILED, captor.getValue().getStatus());
    }

    @Test
    void executeAnalysisAsync_codeMarkdownFences_stripped() throws Exception {
        UUID analysisId = UUID.randomUUID();
        UUID testRunId = UUID.randomUUID();
        TestRun testRun = buildTestRun(testRunId, 1);

        FailureAnalysis analysis = FailureAnalysis.builder()
                .testRun(testRun)
                .status(FailureAnalysis.AnalysisStatus.ANALYZING)
                .build();
        analysis.setId(analysisId);

        when(failureAnalysisRepository.findByIdAndDeletedFalse(analysisId)).thenReturn(Optional.of(analysis));
        when(testRunRepository.findByIdAndDeletedFalse(testRunId)).thenReturn(Optional.of(testRun));
        when(promptBuilder.buildUserPrompt(testRun)).thenReturn("test prompt");

        String aiResponse = """
                ```json
                {"failure_category":"FLAKY_TEST","summary":"Intermittent failure","root_cause":"Race condition","suggested_fix":"Add synchronization","confidence_score":0.75}
                ```""";
        when(anthropicClient.analyze(any(), any()))
                .thenReturn(new AnthropicClient.AnalysisResult(aiResponse, "claude-sonnet-4-20250514", 80, 150));
        when(failureAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        processor.executeAnalysisAsync(analysisId, testRunId);

        ArgumentCaptor<FailureAnalysis> captor = ArgumentCaptor.forClass(FailureAnalysis.class);
        verify(failureAnalysisRepository).save(captor.capture());
        assertEquals(FailureAnalysis.FailureCategory.FLAKY_TEST, captor.getValue().getFailureCategory());
    }

    // ---- Helpers ----

    private TestRun buildTestRun(UUID id, int failedCount) {
        TestRun testRun = TestRun.builder()
                .name("Test Run #1")
                .source(TestRun.TestSource.JUNIT)
                .totalTests(10)
                .passed(10 - failedCount)
                .failed(failedCount)
                .skipped(0)
                .build();
        testRun.setId(id);
        return testRun;
    }
}
