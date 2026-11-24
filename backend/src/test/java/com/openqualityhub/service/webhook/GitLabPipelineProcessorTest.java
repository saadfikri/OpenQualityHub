package com.openqualityhub.service.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openqualityhub.entity.*;
import com.openqualityhub.repository.TestRunRepository;
import com.openqualityhub.service.impl.TestIdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitLabPipelineProcessorTest {

    @Mock
    private WebhookTestRunCreator testRunCreator;

    @Mock
    private GitLabApiClient gitLabApiClient;

    @Mock
    private TestRunRepository testRunRepository;

    @Mock
    private TestIdentityService testIdentityService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private GitLabPipelineProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new GitLabPipelineProcessor(
                testRunCreator, gitLabApiClient, objectMapper, testRunRepository, testIdentityService);
    }

    @Test
    void getProvider_shouldReturnGitLab() {
        assertEquals(IntegrationConfig.Provider.GITLAB, processor.getProvider());
    }

    @Test
    void supports_shouldAcceptPipelineHook() {
        assertTrue(processor.supports("Pipeline Hook"));
    }

    @Test
    void supports_shouldAcceptJobHook() {
        assertTrue(processor.supports("Job Hook"));
    }

    @Test
    void supports_shouldRejectPush() {
        assertFalse(processor.supports("push"));
    }

    @Test
    void supports_shouldRejectNull() {
        assertFalse(processor.supports(null));
    }

    @Test
    void process_customPayloadWithTestResults_shouldDelegateToCreator() throws Exception {
        String payload = "{\"test_results\":[{\"name\":\"test1\",\"status\":\"passed\"}]}";
        WebhookEvent event = buildEvent(payload);

        TestRun mockRun = TestRun.builder().name("mock").build();
        mockRun.setId(UUID.randomUUID());
        when(testRunCreator.createFromPayload(any(), any())).thenReturn(mockRun);

        processor.process(event);

        verify(testRunCreator).createFromPayload(event, TestRun.TestSource.JUNIT);
        verify(gitLabApiClient, never()).fetchTestReport(anyString(), anyString(), anyLong(), anyLong());
    }

    @Test
    void process_noCredentials_shouldSkipApiFetch() throws Exception {
        String payload = pipelinePayload("success");
        IntegrationConfig config = buildConfig("{}");
        WebhookEvent event = buildEvent(payload, config);

        processor.process(event);

        verify(gitLabApiClient, never()).fetchTestReport(anyString(), anyString(), anyLong(), anyLong());
    }

    @Test
    void process_pendingPipeline_shouldSkip() throws Exception {
        String payload = pipelinePayload("pending");
        IntegrationConfig config = buildConfig("{\"gitlab_url\":\"https://gitlab.com\",\"api_token\":\"glpat-xxx\"}");
        WebhookEvent event = buildEvent(payload, config);

        processor.process(event);

        verify(gitLabApiClient, never()).fetchTestReport(anyString(), anyString(), anyLong(), anyLong());
    }

    @Test
    void process_successPipeline_noTestReport_shouldSkip() throws Exception {
        String payload = pipelinePayload("success");
        IntegrationConfig config = buildConfig("{\"gitlab_url\":\"https://gitlab.com\",\"api_token\":\"glpat-xxx\"}");
        WebhookEvent event = buildEvent(payload, config);

        when(gitLabApiClient.fetchTestReport(anyString(), anyString(), anyLong(), anyLong())).thenReturn(null);

        processor.process(event);

        verify(gitLabApiClient).fetchTestReport("https://gitlab.com", "glpat-xxx", 123L, 456L);
        verify(testRunRepository, never()).save(any());
    }

    @Test
    void process_successPipeline_withTestReport_shouldCreateTestRun() throws Exception {
        String payload = pipelinePayload("success");
        Project project = new Project();
        project.setName("my-project");
        IntegrationConfig config = buildConfig(
                "{\"gitlab_url\":\"https://gitlab.com\",\"api_token\":\"glpat-xxx\"}",
                project);
        WebhookEvent event = buildEvent(payload, config);

        String reportJson = """
                {
                  "total_count": 3,
                  "test_suites": [{
                    "name": "MySuite",
                    "test_cases": [
                      {"name": "test_pass", "classname": "com.Foo", "status": "success", "execution_time": 1.5},
                      {"name": "test_fail", "classname": "com.Foo", "status": "failed", "execution_time": 0.3, "system_output": "AssertionError\\nstack trace here"},
                      {"name": "test_skip", "classname": "com.Bar", "status": "skipped", "execution_time": 0.0}
                    ]
                  }]
                }
                """;
        JsonNode report = objectMapper.readTree(reportJson);
        when(gitLabApiClient.fetchTestReport(anyString(), anyString(), anyLong(), anyLong())).thenReturn(report);

        TestRun savedRun = TestRun.builder().name("saved").build();
        savedRun.setId(UUID.randomUUID());
        when(testRunRepository.save(any(TestRun.class))).thenReturn(savedRun);

        processor.process(event);

        ArgumentCaptor<TestRun> captor = ArgumentCaptor.forClass(TestRun.class);
        verify(testRunRepository).save(captor.capture());
        TestRun captured = captor.getValue();

        assertEquals(3, captured.getTotalTests());
        assertEquals(1, captured.getPassed());
        assertEquals(1, captured.getFailed());
        assertEquals(1, captured.getSkipped());
        assertEquals(3, captured.getTestCases().size());
        assertEquals("main", captured.getBranch());
        assertEquals(TestRun.TestSource.JUNIT, captured.getSource());

        verify(testIdentityService).recordExecutions(savedRun);
    }

    @Test
    void process_failedPipeline_shouldAlsoProcess() throws Exception {
        String payload = pipelinePayload("failed");
        IntegrationConfig config = buildConfig("{\"gitlab_url\":\"https://gitlab.com\",\"api_token\":\"glpat-xxx\"}");
        WebhookEvent event = buildEvent(payload, config);

        String reportJson = "{\"total_count\": 0, \"test_suites\": []}";
        when(gitLabApiClient.fetchTestReport(anyString(), anyString(), anyLong(), anyLong()))
                .thenReturn(objectMapper.readTree(reportJson));

        processor.process(event);

        verify(gitLabApiClient).fetchTestReport("https://gitlab.com", "glpat-xxx", 123L, 456L);
    }

    @Test
    void process_noProject_shouldSkip() throws Exception {
        String payload = pipelinePayload("success");
        IntegrationConfig config = buildConfig("{\"gitlab_url\":\"https://gitlab.com\",\"api_token\":\"glpat-xxx\"}");
        config.setProject(null);
        WebhookEvent event = buildEvent(payload, config);

        String reportJson = "{\"total_count\": 1, \"test_suites\": [{\"name\":\"S\",\"test_cases\":[{\"name\":\"t\",\"status\":\"success\"}]}]}";
        when(gitLabApiClient.fetchTestReport(anyString(), anyString(), anyLong(), anyLong()))
                .thenReturn(objectMapper.readTree(reportJson));

        processor.process(event);

        verify(testRunRepository, never()).save(any());
    }

    // --- Helpers ---

    private WebhookEvent buildEvent(String payload) {
        return buildEvent(payload, null);
    }

    private WebhookEvent buildEvent(String payload, IntegrationConfig config) {
        WebhookEvent event = WebhookEvent.builder()
                .eventType("Pipeline Hook")
                .provider(IntegrationConfig.Provider.GITLAB)
                .payload(payload)
                .integration(config)
                .build();
        event.setId(UUID.randomUUID());
        return event;
    }

    private IntegrationConfig buildConfig(String configJson) {
        return buildConfig(configJson, new Project());
    }

    private IntegrationConfig buildConfig(String configJson, Project project) {
        IntegrationConfig config = new IntegrationConfig();
        config.setId(UUID.randomUUID());
        config.setConfig(configJson);
        config.setProject(project);
        return config;
    }

    private String pipelinePayload(String status) {
        return String.format("""
                {
                  "object_kind": "pipeline",
                  "object_attributes": {
                    "id": 456,
                    "ref": "main",
                    "sha": "abc123",
                    "status": "%s"
                  },
                  "project": {
                    "id": 123,
                    "name": "my-project"
                  }
                }
                """, status);
    }
}
