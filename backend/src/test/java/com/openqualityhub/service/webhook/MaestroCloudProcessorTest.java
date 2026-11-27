package com.openqualityhub.service.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openqualityhub.entity.*;
import com.openqualityhub.repository.MaestroFlowResultRepository;
import com.openqualityhub.repository.TestRunRepository;
import com.openqualityhub.service.impl.TestIdentityService;
import com.openqualityhub.service.impl.parser.MaestroJsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaestroCloudProcessorTest {

    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @Mock private MaestroJsonParser maestroParser;
    @Mock private TestRunRepository testRunRepository;
    @Mock private MaestroFlowResultRepository flowResultRepository;
    @Mock private TestIdentityService testIdentityService;

    @InjectMocks
    private MaestroCloudProcessor processor;

    private IntegrationConfig config;
    private Project project;

    @BeforeEach
    void setUp() {
        project = Project.builder().name("Mobile App").build();
        project.setId(UUID.randomUUID());

        Organization org = Organization.builder().name("Test Org").slug("test-org").build();
        org.setId(UUID.randomUUID());

        config = IntegrationConfig.builder()
            .provider(IntegrationConfig.Provider.MAESTRO)
            .organization(org)
            .project(project)
            .name("Maestro Cloud")
            .enabled(true)
            .build();
        config.setId(UUID.randomUUID());
    }

    @Test
    void getProvider_returnsMaestro() {
        assertEquals(IntegrationConfig.Provider.MAESTRO, processor.getProvider());
    }

    @Test
    void supports_flowComplete() {
        assertTrue(processor.supports("flow_complete"));
        assertTrue(processor.supports("suite_complete"));
        assertTrue(processor.supports("test_complete"));
        assertTrue(processor.supports("upload_complete"));
        assertFalse(processor.supports("unknown"));
        assertFalse(processor.supports("push"));
    }

    @Test
    void process_createsTestRunAndFlowResults() throws Exception {
        String payload = """
            {
              "name": "Nightly Mobile Suite",
              "environment": "staging",
              "commit_sha": "abc123",
              "branch": "main",
              "flows": [
                {"name": "Login", "status": "passed", "duration_ms": 3000}
              ]
            }
            """;

        WebhookEvent event = WebhookEvent.builder()
            .eventType("suite_complete")
            .provider(IntegrationConfig.Provider.MAESTRO)
            .payload(payload)
            .integration(config)
            .build();
        event.setId(UUID.randomUUID());

        TestRun mockTestRun = TestRun.builder()
            .name("Nightly Mobile Suite")
            .source(TestRun.TestSource.MAESTRO)
            .totalTests(1)
            .passed(1)
            .project(project)
            .testCases(new ArrayList<>())
            .build();
        mockTestRun.setId(UUID.randomUUID());

        MaestroFlowResult mockFlow = MaestroFlowResult.builder()
            .flowName("Login")
            .status(MaestroFlowResult.FlowStatus.PASSED)
            .testRun(mockTestRun)
            .build();

        when(maestroParser.parse(any())).thenReturn(
            new MaestroJsonParser.ParseResult(mockTestRun, List.of(mockFlow)));
        when(testRunRepository.save(any(TestRun.class))).thenReturn(mockTestRun);

        processor.process(event);

        verify(testRunRepository).save(any(TestRun.class));
        verify(flowResultRepository).saveAll(anyList());
    }

    @Test
    void process_noProject_skips() throws Exception {
        config.setProject(null);

        WebhookEvent event = WebhookEvent.builder()
            .eventType("suite_complete")
            .provider(IntegrationConfig.Provider.MAESTRO)
            .payload("{}")
            .integration(config)
            .build();
        event.setId(UUID.randomUUID());

        processor.process(event);

        verify(testRunRepository, never()).save(any());
    }

    @Test
    void process_extractsGitInfo() throws Exception {
        String payload = """
            {
              "name": "CI Run",
              "git": {"sha": "def456", "branch": "feature/checkout"},
              "flows": []
            }
            """;

        WebhookEvent event = WebhookEvent.builder()
            .eventType("suite_complete")
            .provider(IntegrationConfig.Provider.MAESTRO)
            .payload(payload)
            .integration(config)
            .build();
        event.setId(UUID.randomUUID());

        TestRun mockTestRun = TestRun.builder()
            .source(TestRun.TestSource.MAESTRO)
            .totalTests(0)
            .testCases(new ArrayList<>())
            .build();
        mockTestRun.setId(UUID.randomUUID());

        when(maestroParser.parse(any())).thenReturn(
            new MaestroJsonParser.ParseResult(mockTestRun, List.of()));
        when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
            TestRun tr = inv.getArgument(0);
            assertEquals("def456", tr.getCommitSha());
            assertEquals("feature/checkout", tr.getBranch());
            return tr;
        });

        processor.process(event);

        verify(testRunRepository).save(any(TestRun.class));
    }
}
