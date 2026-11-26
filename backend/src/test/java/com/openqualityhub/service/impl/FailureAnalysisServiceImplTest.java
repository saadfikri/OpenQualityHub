package com.openqualityhub.service.impl;

import com.openqualityhub.dto.request.SubmitFeedbackRequest;
import com.openqualityhub.dto.response.FailureAnalysisResponse;
import com.openqualityhub.entity.FailureAnalysis;
import com.openqualityhub.entity.TestRun;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.mapper.FailureAnalysisMapper;
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
class FailureAnalysisServiceImplTest {

    @Mock private FailureAnalysisRepository failureAnalysisRepository;
    @Mock private TestRunRepository testRunRepository;
    @Mock private AnthropicClient anthropicClient;
    @Mock private FailureAnalysisMapper mapper;
    @Mock private FailureAnalysisAsyncProcessor asyncProcessor;

    private FailureAnalysisServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FailureAnalysisServiceImpl(
                failureAnalysisRepository, testRunRepository,
                anthropicClient, mapper, asyncProcessor);
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

        assertEquals("ANALYZING", resp.getStatus());

        ArgumentCaptor<FailureAnalysis> captor = ArgumentCaptor.forClass(FailureAnalysis.class);
        verify(failureAnalysisRepository).save(captor.capture());
        assertEquals(FailureAnalysis.AnalysisStatus.ANALYZING, captor.getValue().getStatus());

        // Verify async processor was invoked
        verify(asyncProcessor).executeAnalysisAsync(any(UUID.class), eq(testRunId));
    }

    @Test
    void triggerAnalysis_aiDisabled_skipped() {
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
        verify(asyncProcessor, never()).executeAnalysisAsync(any(), any());
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
