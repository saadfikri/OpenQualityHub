package com.openqualityhub.service.impl;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FailureAnalysisServiceImplTest {

    @Mock private FailureAnalysisRepository failureAnalysisRepository;
    @Mock private TestRunRepository testRunRepository;
    @Mock private TestCaseRepository testCaseRepository;
    @Mock private AnthropicClient anthropicClient;
    @Mock private FailureAnalysisMapper mapper;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private FailureAnalysisServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FailureAnalysisServiceImpl(
                failureAnalysisRepository, testRunRepository, testCaseRepository,
                anthropicClient, mapper, objectMapper);
    }

    // ---- triggerAnalysis tests ----

    @Test
    void triggerAnalysis_aiEnabled_returnsAnalyzingImmediately() {
        UUID testRunId = UUID.randomUUID();
        TestRun testRun = buildTestRun(testRunId, 3);

        when(testRunRepository.findByIdAndDeletedFalse(testRunId)).thenReturn(Optional.of(testRun));
        when(failureAnalysisRepository.findByTestRunIdAndDeletedFalse(testRunId)).thenReturn(Optional.empty());
        when(anthropicClient.isEnabled()).thenReturn(true);
        when(failureAnalysisRepository.save(any())).thenAnswer(inv -> {
            FailureAnalysis a = inv.getArgument(0);
            if (a.getId() == null) a.setId(UUID.randomUUID());
            return a;
        });

        FailureAnalysisResponse mockResponse = FailureAnalysisResponse.builder()
                .id(UUID.randomUUID()).status("ANALYZING").build();
        when(mapper.toResponse(any())).thenReturn(mockResponse);

        FailureAnalysisResponse resp = service.triggerAnalysis(testRunId);

        // Returns immediately with ANALYZING status (async processing happens separately)
        assertEquals("ANALYZING", resp.getStatus());

        // Verify the analysis was saved with ANALYZING status
        ArgumentCaptor<FailureAnalysis> captor = ArgumentCaptor.forClass(FailureAnalysis.class);
        verify(failureAnalysisRepository).save(captor.capture());
        assertEquals(FailureAnalysis.AnalysisStatus.ANALYZING, captor.getValue().getStatus());
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

        TestCase failedCase = TestCase.builder()
                .name("testLogin").className("AuthTest")
                .status(TestCase.TestStatus.FAILED)
                .errorMessage("NullPointerException").stackTrace("at AuthTest.testLogin:42")
                .build();
        when(testCaseRepository.findByTestRunIdAndStatus(eq(testRunId), eq(TestCase.TestStatus.FAILED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(failedCase)));

        String aiResponse = """
                {"failure_category":"CODE_REGRESSION","summary":"NPE in login","root_cause":"Null user object","suggested_fix":"Check user initialization","confidence_score":0.9}
                """;
        when(anthropicClient.analyze(any(), any()))
                .thenReturn(new AnthropicClient.AnalysisResult(aiResponse, "claude-sonnet-4-20250514", 100, 200));

        when(failureAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.executeAnalysisAsync(analysisId, testRunId);

        ArgumentCaptor<FailureAnalysis> captor = ArgumentCaptor.forClass(FailureAnalysis.class);
        verify(failureAnalysisRepository).save(captor.capture());
        FailureAnalysis saved = captor.getValue();
        assertEquals(FailureAnalysis.AnalysisStatus.COMPLETED, saved.getStatus());
        assertEquals(FailureAnalysis.FailureCategory.CODE_REGRESSION, saved.getFailureCategory());
        assertEquals("NPE in login", saved.getSummary());
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
        when(testCaseRepository.findByTestRunIdAndStatus(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(anthropicClient.analyze(any(), any())).thenThrow(new RuntimeException("API timeout"));
        when(failureAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.executeAnalysisAsync(analysisId, testRunId);

        ArgumentCaptor<FailureAnalysis> captor = ArgumentCaptor.forClass(FailureAnalysis.class);
        verify(failureAnalysisRepository).save(captor.capture());
        assertEquals(FailureAnalysis.AnalysisStatus.FAILED, captor.getValue().getStatus());
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
        when(testCaseRepository.findByTestRunIdAndStatus(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(anthropicClient.analyze(any(), any()))
                .thenReturn(new AnthropicClient.AnalysisResult("not valid json at all", "claude-sonnet-4-20250514", 50, 30));
        when(failureAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.executeAnalysisAsync(analysisId, testRunId);

        ArgumentCaptor<FailureAnalysis> captor = ArgumentCaptor.forClass(FailureAnalysis.class);
        verify(failureAnalysisRepository).save(captor.capture());
        FailureAnalysis last = captor.getValue();
        assertEquals(FailureAnalysis.AnalysisStatus.COMPLETED, last.getStatus());
        assertEquals(FailureAnalysis.FailureCategory.UNKNOWN, last.getFailureCategory());
    }

    @Test
    void triggerAnalysis_aiDisabled_skipped() throws Exception {
        UUID testRunId = UUID.randomUUID();
        TestRun testRun = buildTestRun(testRunId, 2);

        when(testRunRepository.findByIdAndDeletedFalse(testRunId)).thenReturn(Optional.of(testRun));
        when(failureAnalysisRepository.findByTestRunIdAndDeletedFalse(testRunId)).thenReturn(Optional.empty());
        when(anthropicClient.isEnabled()).thenReturn(false);
        when(failureAnalysisRepository.save(any())).thenAnswer(inv -> {
            FailureAnalysis a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        when(mapper.toResponse(any())).thenReturn(FailureAnalysisResponse.builder().status("SKIPPED").build());

        FailureAnalysisResponse resp = service.triggerAnalysis(testRunId);

        assertEquals("SKIPPED", resp.getStatus());
        verify(anthropicClient, never()).analyze(any(), any());
    }

    @Test
    void triggerAnalysis_noFailures_throwsException() {
        UUID testRunId = UUID.randomUUID();
        TestRun testRun = TestRun.builder().name("clean run").failed(0).build();
        testRun.setId(testRunId);

        when(testRunRepository.findByIdAndDeletedFalse(testRunId)).thenReturn(Optional.of(testRun));

        assertThrows(IllegalArgumentException.class, () -> service.triggerAnalysis(testRunId));
    }

    @Test
    void triggerAnalysis_testRunNotFound_throwsException() {
        UUID testRunId = UUID.randomUUID();
        when(testRunRepository.findByIdAndDeletedFalse(testRunId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.triggerAnalysis(testRunId));
    }

    @Test
    void triggerAnalysis_alreadyExists_throwsException() {
        UUID testRunId = UUID.randomUUID();
        TestRun testRun = buildTestRun(testRunId, 1);

        when(testRunRepository.findByIdAndDeletedFalse(testRunId)).thenReturn(Optional.of(testRun));
        when(failureAnalysisRepository.findByTestRunIdAndDeletedFalse(testRunId))
                .thenReturn(Optional.of(FailureAnalysis.builder().build()));

        assertThrows(IllegalArgumentException.class, () -> service.triggerAnalysis(testRunId));
    }

    // ---- getByTestRunId tests ----

    @Test
    void getByTestRunId_success() {
        UUID testRunId = UUID.randomUUID();
        FailureAnalysis analysis = FailureAnalysis.builder().build();
        analysis.setId(UUID.randomUUID());

        when(failureAnalysisRepository.findByTestRunIdAndDeletedFalse(testRunId)).thenReturn(Optional.of(analysis));
        when(mapper.toResponse(analysis)).thenReturn(FailureAnalysisResponse.builder().id(analysis.getId()).build());

        FailureAnalysisResponse resp = service.getByTestRunId(testRunId);
        assertNotNull(resp);
    }

    @Test
    void getByTestRunId_notFound() {
        UUID testRunId = UUID.randomUUID();
        when(failureAnalysisRepository.findByTestRunIdAndDeletedFalse(testRunId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getByTestRunId(testRunId));
    }

    // ---- getById tests ----

    @Test
    void getById_success() {
        UUID id = UUID.randomUUID();
        FailureAnalysis analysis = FailureAnalysis.builder().build();
        analysis.setId(id);

        when(failureAnalysisRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(analysis));
        when(mapper.toResponse(analysis)).thenReturn(FailureAnalysisResponse.builder().id(id).build());

        FailureAnalysisResponse resp = service.getById(id);
        assertEquals(id, resp.getId());
    }

    // ---- submitFeedback tests ----

    @Test
    void submitFeedback_accepted() {
        UUID analysisId = UUID.randomUUID();
        FailureAnalysis analysis = FailureAnalysis.builder()
                .status(FailureAnalysis.AnalysisStatus.COMPLETED)
                .humanFeedback(FailureAnalysis.HumanFeedback.PENDING)
                .build();
        analysis.setId(analysisId);

        when(failureAnalysisRepository.findByIdAndDeletedFalse(analysisId)).thenReturn(Optional.of(analysis));
        when(failureAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(FailureAnalysisResponse.builder()
                .humanFeedback("ACCEPTED").build());

        SubmitFeedbackRequest request = SubmitFeedbackRequest.builder()
                .feedback("ACCEPTED").comment("Looks correct").build();

        FailureAnalysisResponse resp = service.submitFeedback(analysisId, request);
        assertEquals("ACCEPTED", resp.getHumanFeedback());

        ArgumentCaptor<FailureAnalysis> captor = ArgumentCaptor.forClass(FailureAnalysis.class);
        verify(failureAnalysisRepository).save(captor.capture());
        assertEquals(FailureAnalysis.HumanFeedback.ACCEPTED, captor.getValue().getHumanFeedback());
        assertEquals("Looks correct", captor.getValue().getFeedbackComment());
    }

    @Test
    void submitFeedback_notCompleted_throwsException() {
        UUID analysisId = UUID.randomUUID();
        FailureAnalysis analysis = FailureAnalysis.builder()
                .status(FailureAnalysis.AnalysisStatus.ANALYZING)
                .build();
        analysis.setId(analysisId);

        when(failureAnalysisRepository.findByIdAndDeletedFalse(analysisId)).thenReturn(Optional.of(analysis));

        SubmitFeedbackRequest request = SubmitFeedbackRequest.builder().feedback("ACCEPTED").build();
        assertThrows(IllegalArgumentException.class, () -> service.submitFeedback(analysisId, request));
    }

    @Test
    void submitFeedback_invalidFeedback_throwsException() {
        UUID analysisId = UUID.randomUUID();
        FailureAnalysis analysis = FailureAnalysis.builder()
                .status(FailureAnalysis.AnalysisStatus.COMPLETED)
                .build();
        analysis.setId(analysisId);

        when(failureAnalysisRepository.findByIdAndDeletedFalse(analysisId)).thenReturn(Optional.of(analysis));

        SubmitFeedbackRequest request = SubmitFeedbackRequest.builder().feedback("INVALID").build();
        assertThrows(IllegalArgumentException.class, () -> service.submitFeedback(analysisId, request));
    }

    @Test
    void submitFeedback_pendingFeedback_throwsException() {
        UUID analysisId = UUID.randomUUID();
        FailureAnalysis analysis = FailureAnalysis.builder()
                .status(FailureAnalysis.AnalysisStatus.COMPLETED)
                .build();
        analysis.setId(analysisId);

        when(failureAnalysisRepository.findByIdAndDeletedFalse(analysisId)).thenReturn(Optional.of(analysis));

        SubmitFeedbackRequest request = SubmitFeedbackRequest.builder().feedback("PENDING").build();
        assertThrows(IllegalArgumentException.class, () -> service.submitFeedback(analysisId, request));
    }

    // ---- isAvailable test ----

    @Test
    void isAvailable_delegatesToClient() {
        when(anthropicClient.isEnabled()).thenReturn(true);
        assertTrue(service.isAvailable());

        when(anthropicClient.isEnabled()).thenReturn(false);
        assertFalse(service.isAvailable());
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
