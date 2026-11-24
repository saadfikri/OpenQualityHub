package com.openqualityhub.service.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openqualityhub.entity.*;
import com.openqualityhub.repository.ProjectRepository;
import com.openqualityhub.repository.TestRunRepository;
import com.openqualityhub.service.impl.TestIdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookTestRunCreatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private TestRunRepository testRunRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TestIdentityService testIdentityService;

    private WebhookTestRunCreator creator;

    @BeforeEach
    void setUp() {
        creator = new WebhookTestRunCreator(objectMapper, testRunRepository, projectRepository, testIdentityService);
    }

    private WebhookEvent buildEvent(String payload) {
        Project project = new Project();
        project.setName("Test Project");

        IntegrationConfig config = IntegrationConfig.builder()
            .provider(IntegrationConfig.Provider.GITHUB)
            .name("Test Integration")
            .project(project)
            .build();
        config.setId(UUID.randomUUID());

        WebhookEvent event = WebhookEvent.builder()
            .eventType("workflow_run")
            .provider(IntegrationConfig.Provider.GITHUB)
            .payload(payload)
            .integration(config)
            .build();
        event.setId(UUID.randomUUID());
        return event;
    }

    @Nested
    class CreateFromPayloadWithSuites {

        @Test
        void shouldCreateTestRunWithTestCasesFromSuites() {
            String payload = """
                {
                  "test_results": {
                    "name": "CI Build #42",
                    "environment": "staging",
                    "suites": [
                      {
                        "name": "AuthSuite",
                        "tests": [
                          {"name": "testLogin", "status": "passed", "duration_ms": 120},
                          {"name": "testLogout", "status": "failed", "duration_ms": 80, "error": "Assertion failed"},
                          {"name": "testRegister", "status": "skipped", "duration_ms": 0}
                        ]
                      }
                    ]
                  },
                  "head_commit": {"id": "abc123def456"}
                }
                """;

            WebhookEvent event = buildEvent(payload);
            when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                TestRun tr = inv.getArgument(0);
                tr.setId(UUID.randomUUID());
                return tr;
            });

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.PLAYWRIGHT);

            assertNotNull(result);
            assertEquals("CI Build #42", result.getName());
            assertEquals(TestRun.TestSource.PLAYWRIGHT, result.getSource());
            assertEquals("staging", result.getEnvironment());
            assertEquals("abc123def456", result.getCommitSha());
            assertEquals(3, result.getTotalTests());
            assertEquals(1, result.getPassed());
            assertEquals(1, result.getFailed());
            assertEquals(1, result.getSkipped());
            assertEquals(200L, result.getDurationMs());
            assertEquals(3, result.getTestCases().size());

            verify(testRunRepository).save(any(TestRun.class));
            verify(testIdentityService).recordExecutions(result);
        }

        @Test
        void shouldHandleMultipleSuites() {
            String payload = """
                {
                  "test_results": {
                    "name": "Full Suite",
                    "suites": [
                      {
                        "name": "Suite A",
                        "tests": [
                          {"name": "test1", "status": "passed", "duration_ms": 50}
                        ]
                      },
                      {
                        "name": "Suite B",
                        "tests": [
                          {"name": "test2", "status": "failed", "duration_ms": 100, "error": {"message": "NPE", "stack": "at Foo.java:10"}}
                        ]
                      }
                    ]
                  }
                }
                """;

            WebhookEvent event = buildEvent(payload);
            when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                TestRun tr = inv.getArgument(0);
                tr.setId(UUID.randomUUID());
                return tr;
            });

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

            assertNotNull(result);
            assertEquals(2, result.getTotalTests());
            assertEquals(1, result.getPassed());
            assertEquals(1, result.getFailed());
            assertEquals(0, result.getSkipped());
            assertEquals(150L, result.getDurationMs());

            // Verify test cases have correct suite names
            assertEquals(2, result.getTestCases().size());
            assertTrue(result.getTestCases().stream().anyMatch(tc -> "Suite A".equals(tc.getSuiteName())));
            assertTrue(result.getTestCases().stream().anyMatch(tc -> "Suite B".equals(tc.getSuiteName())));
        }

        @Test
        void shouldParseErrorObjectWithMessageAndStack() {
            String payload = """
                {
                  "test_results": {
                    "suites": [
                      {
                        "name": "ErrorSuite",
                        "tests": [
                          {
                            "name": "testWithError",
                            "status": "failed",
                            "duration_ms": 200,
                            "error": {
                              "message": "Expected 200 but got 500",
                              "stack": "at com.example.Test.run(Test.java:42)"
                            }
                          }
                        ]
                      }
                    ]
                  }
                }
                """;

            WebhookEvent event = buildEvent(payload);
            when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                TestRun tr = inv.getArgument(0);
                tr.setId(UUID.randomUUID());
                return tr;
            });

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

            assertNotNull(result);
            TestCase failedCase = result.getTestCases().get(0);
            assertEquals("Expected 200 but got 500", failedCase.getErrorMessage());
            assertEquals("at com.example.Test.run(Test.java:42)", failedCase.getStackTrace());
        }

        @Test
        void shouldParseErrorAsPlainString() {
            String payload = """
                {
                  "test_results": {
                    "suites": [
                      {
                        "name": "Suite",
                        "tests": [
                          {"name": "testFail", "status": "failed", "duration_ms": 10, "error": "Simple error message"}
                        ]
                      }
                    ]
                  }
                }
                """;

            WebhookEvent event = buildEvent(payload);
            when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                TestRun tr = inv.getArgument(0);
                tr.setId(UUID.randomUUID());
                return tr;
            });

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

            assertNotNull(result);
            TestCase failedCase = result.getTestCases().get(0);
            assertEquals("Simple error message", failedCase.getErrorMessage());
            assertNull(failedCase.getStackTrace());
        }

        @Test
        void shouldUseDefaultNameWhenNotProvided() {
            String payload = """
                {
                  "test_results": {
                    "suites": [
                      {
                        "tests": [
                          {"name": "test1", "status": "passed", "duration_ms": 10}
                        ]
                      }
                    ]
                  }
                }
                """;

            WebhookEvent event = buildEvent(payload);
            when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                TestRun tr = inv.getArgument(0);
                tr.setId(UUID.randomUUID());
                return tr;
            });

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.PLAYWRIGHT);

            assertNotNull(result);
            assertTrue(result.getName().startsWith("Webhook Import #"));
            // Suite name should default to "Default Suite"
            assertEquals("Default Suite", result.getTestCases().get(0).getSuiteName());
        }

        @Test
        void shouldNotCallRecordExecutionsWhenSuitesAreEmpty() {
            String payload = """
                {
                  "test_results": {
                    "name": "Empty suites",
                    "suites": []
                  }
                }
                """;

            WebhookEvent event = buildEvent(payload);
            when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                TestRun tr = inv.getArgument(0);
                tr.setId(UUID.randomUUID());
                return tr;
            });

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

            assertNotNull(result);
            assertEquals(0, result.getTotalTests());
            verify(testIdentityService, never()).recordExecutions(any());
        }
    }

    @Nested
    class CreateFromPayloadSummaryOnly {

        @Test
        void shouldCreateTestRunWithSummaryCountsOnly() {
            String payload = """
                {
                  "test_results": {
                    "name": "Nightly Build",
                    "total": 150,
                    "passed": 140,
                    "failed": 8,
                    "skipped": 2,
                    "duration_ms": 45000
                  }
                }
                """;

            WebhookEvent event = buildEvent(payload);
            when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                TestRun tr = inv.getArgument(0);
                tr.setId(UUID.randomUUID());
                return tr;
            });

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.WEBDRIVERIO);

            assertNotNull(result);
            assertEquals("Nightly Build", result.getName());
            assertEquals(TestRun.TestSource.WEBDRIVERIO, result.getSource());
            assertEquals(150, result.getTotalTests());
            assertEquals(140, result.getPassed());
            assertEquals(8, result.getFailed());
            assertEquals(2, result.getSkipped());
            assertEquals(45000L, result.getDurationMs());
            assertTrue(result.getTestCases().isEmpty());

            verify(testRunRepository).save(any(TestRun.class));
            verify(testIdentityService, never()).recordExecutions(any());
        }

        @Test
        void shouldDefaultSummaryCountsToZero() {
            String payload = """
                {
                  "test_results": {
                    "name": "Minimal"
                  }
                }
                """;

            WebhookEvent event = buildEvent(payload);
            when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                TestRun tr = inv.getArgument(0);
                tr.setId(UUID.randomUUID());
                return tr;
            });

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

            assertNotNull(result);
            assertEquals(0, result.getTotalTests());
            assertEquals(0, result.getPassed());
            assertEquals(0, result.getFailed());
            assertEquals(0, result.getSkipped());
            assertEquals(0L, result.getDurationMs());
        }
    }

    @Nested
    class CreateFromPayloadReturnsNull {

        @Test
        void shouldReturnNullWhenPayloadHasNoTestResults() {
            String payload = """
                {
                  "action": "completed",
                  "workflow_run": {"status": "completed"}
                }
                """;

            WebhookEvent event = buildEvent(payload);

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.PLAYWRIGHT);

            assertNull(result);
            verify(testRunRepository, never()).save(any());
            verify(testIdentityService, never()).recordExecutions(any());
        }

        @Test
        void shouldReturnNullWhenTestResultsIsNull() {
            String payload = """
                {
                  "test_results": null
                }
                """;

            WebhookEvent event = buildEvent(payload);

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

            assertNull(result);
            verify(testRunRepository, never()).save(any());
        }

        @Test
        void shouldReturnNullAndLogWarningWhenIntegrationHasNoProject() {
            IntegrationConfig config = IntegrationConfig.builder()
                .provider(IntegrationConfig.Provider.GITHUB)
                .name("No Project Integration")
                .project(null)
                .build();
            config.setId(UUID.randomUUID());

            WebhookEvent event = WebhookEvent.builder()
                .eventType("workflow_run")
                .provider(IntegrationConfig.Provider.GITHUB)
                .payload("""
                    {"test_results": {"total": 10, "passed": 10}}
                    """)
                .integration(config)
                .build();
            event.setId(UUID.randomUUID());

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.PLAYWRIGHT);

            assertNull(result);
            verify(testRunRepository, never()).save(any());
        }

        @Test
        void shouldReturnNullWhenExceptionOccurs() {
            WebhookEvent event = WebhookEvent.builder()
                .eventType("workflow_run")
                .provider(IntegrationConfig.Provider.GITHUB)
                .payload("THIS IS NOT VALID JSON {{{")
                .integration(IntegrationConfig.builder()
                    .provider(IntegrationConfig.Provider.GITHUB)
                    .name("Bad Integration")
                    .build())
                .build();
            event.setId(UUID.randomUUID());

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

            assertNull(result);
            verify(testRunRepository, never()).save(any());
        }
    }

    @Nested
    class TestStatusMapping {

        private TestRun createRunWithStatus(String statusStr) {
            String payload = String.format("""
                {
                  "test_results": {
                    "suites": [
                      {
                        "name": "StatusSuite",
                        "tests": [
                          {"name": "statusTest", "status": "%s", "duration_ms": 10}
                        ]
                      }
                    ]
                  }
                }
                """, statusStr);

            WebhookEvent event = buildEvent(payload);
            when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                TestRun tr = inv.getArgument(0);
                tr.setId(UUID.randomUUID());
                return tr;
            });

            return creator.createFromPayload(event, TestRun.TestSource.JUNIT);
        }

        @Test
        void shouldMapPassedVariants() {
            for (String status : new String[]{"passed", "pass", "success"}) {
                reset(testRunRepository);
                when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                    TestRun tr = inv.getArgument(0);
                    tr.setId(UUID.randomUUID());
                    return tr;
                });

                String payload = String.format("""
                    {
                      "test_results": {
                        "suites": [{"name": "S", "tests": [{"name": "t", "status": "%s", "duration_ms": 1}]}]
                      }
                    }
                    """, status);
                WebhookEvent event = buildEvent(payload);
                TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

                assertNotNull(result, "Result should not be null for status: " + status);
                assertEquals(TestCase.TestStatus.PASSED, result.getTestCases().get(0).getStatus(),
                    "Status '" + status + "' should map to PASSED");
                assertEquals(1, result.getPassed());
            }
        }

        @Test
        void shouldMapFailedVariants() {
            for (String status : new String[]{"failed", "fail", "failure"}) {
                reset(testRunRepository);
                when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                    TestRun tr = inv.getArgument(0);
                    tr.setId(UUID.randomUUID());
                    return tr;
                });

                String payload = String.format("""
                    {
                      "test_results": {
                        "suites": [{"name": "S", "tests": [{"name": "t", "status": "%s", "duration_ms": 1}]}]
                      }
                    }
                    """, status);
                WebhookEvent event = buildEvent(payload);
                TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

                assertNotNull(result);
                assertEquals(TestCase.TestStatus.FAILED, result.getTestCases().get(0).getStatus(),
                    "Status '" + status + "' should map to FAILED");
                assertEquals(1, result.getFailed());
            }
        }

        @Test
        void shouldMapSkippedVariants() {
            for (String status : new String[]{"skipped", "skip", "pending", "disabled"}) {
                reset(testRunRepository);
                when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                    TestRun tr = inv.getArgument(0);
                    tr.setId(UUID.randomUUID());
                    return tr;
                });

                String payload = String.format("""
                    {
                      "test_results": {
                        "suites": [{"name": "S", "tests": [{"name": "t", "status": "%s", "duration_ms": 1}]}]
                      }
                    }
                    """, status);
                WebhookEvent event = buildEvent(payload);
                TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

                assertNotNull(result);
                assertEquals(TestCase.TestStatus.SKIPPED, result.getTestCases().get(0).getStatus(),
                    "Status '" + status + "' should map to SKIPPED");
                assertEquals(1, result.getSkipped());
            }
        }

        @Test
        void shouldMapErrorVariants() {
            for (String status : new String[]{"error", "broken", "timedout"}) {
                reset(testRunRepository);
                when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                    TestRun tr = inv.getArgument(0);
                    tr.setId(UUID.randomUUID());
                    return tr;
                });

                String payload = String.format("""
                    {
                      "test_results": {
                        "suites": [{"name": "S", "tests": [{"name": "t", "status": "%s", "duration_ms": 1}]}]
                      }
                    }
                    """, status);
                WebhookEvent event = buildEvent(payload);
                TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

                assertNotNull(result);
                assertEquals(TestCase.TestStatus.ERROR, result.getTestCases().get(0).getStatus(),
                    "Status '" + status + "' should map to ERROR");
                // ERROR counts as failed
                assertEquals(1, result.getFailed());
            }
        }

        @Test
        void shouldMapUnknownStatusToFailed() {
            TestRun result = createRunWithStatus("unknown_status");

            assertNotNull(result);
            assertEquals(TestCase.TestStatus.FAILED, result.getTestCases().get(0).getStatus());
            assertEquals(1, result.getFailed());
        }
    }

    @Nested
    class ExtractCommitSha {

        @Test
        void shouldExtractFromGitHubHeadCommit() {
            String payload = """
                {
                  "head_commit": {"id": "sha-from-head-commit"},
                  "test_results": {"total": 1, "passed": 1}
                }
                """;

            WebhookEvent event = buildEvent(payload);
            when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                TestRun tr = inv.getArgument(0);
                tr.setId(UUID.randomUUID());
                return tr;
            });

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

            assertNotNull(result);
            assertEquals("sha-from-head-commit", result.getCommitSha());
        }

        @Test
        void shouldExtractFromGitHubWorkflowRun() {
            String payload = """
                {
                  "workflow_run": {"head_sha": "sha-from-workflow", "head_branch": "main"},
                  "test_results": {"total": 1, "passed": 1}
                }
                """;

            WebhookEvent event = buildEvent(payload);
            when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                TestRun tr = inv.getArgument(0);
                tr.setId(UUID.randomUUID());
                return tr;
            });

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

            assertNotNull(result);
            assertEquals("sha-from-workflow", result.getCommitSha());
        }

        @Test
        void shouldExtractFromGitLabCheckoutSha() {
            String payload = """
                {
                  "checkout_sha": "sha-from-gitlab",
                  "test_results": {"total": 1, "passed": 1}
                }
                """;

            WebhookEvent event = buildEvent(payload);
            when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                TestRun tr = inv.getArgument(0);
                tr.setId(UUID.randomUUID());
                return tr;
            });

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

            assertNotNull(result);
            assertEquals("sha-from-gitlab", result.getCommitSha());
        }

        @Test
        void shouldExtractFromGenericCommitSha() {
            String payload = """
                {
                  "commit_sha": "sha-from-generic",
                  "test_results": {"total": 1, "passed": 1}
                }
                """;

            WebhookEvent event = buildEvent(payload);
            when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                TestRun tr = inv.getArgument(0);
                tr.setId(UUID.randomUUID());
                return tr;
            });

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

            assertNotNull(result);
            assertEquals("sha-from-generic", result.getCommitSha());
        }

        @Test
        void shouldExtractFromTestResultsCommitSha() {
            String payload = """
                {
                  "test_results": {"total": 1, "passed": 1, "commit_sha": "sha-from-test-results"}
                }
                """;

            WebhookEvent event = buildEvent(payload);
            when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                TestRun tr = inv.getArgument(0);
                tr.setId(UUID.randomUUID());
                return tr;
            });

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

            assertNotNull(result);
            assertEquals("sha-from-test-results", result.getCommitSha());
        }

        @Test
        void shouldReturnNullWhenNoCommitShaPresent() {
            String payload = """
                {
                  "test_results": {"total": 1, "passed": 1}
                }
                """;

            WebhookEvent event = buildEvent(payload);
            when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                TestRun tr = inv.getArgument(0);
                tr.setId(UUID.randomUUID());
                return tr;
            });

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

            assertNotNull(result);
            assertNull(result.getCommitSha());
        }
    }

    @Nested
    class ExtractBranch {

        @Test
        void shouldExtractFromGitHubWorkflowRunHeadBranch() {
            String payload = """
                {
                  "workflow_run": {"head_sha": "abc", "head_branch": "feature/login"},
                  "test_results": {"total": 1, "passed": 1}
                }
                """;

            WebhookEvent event = buildEvent(payload);
            when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                TestRun tr = inv.getArgument(0);
                tr.setId(UUID.randomUUID());
                return tr;
            });

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

            assertNotNull(result);
            assertEquals("feature/login", result.getBranch());
        }

        @Test
        void shouldExtractFromGitHubRefWithPrefix() {
            String payload = """
                {
                  "ref": "refs/heads/develop",
                  "head_commit": {"id": "abc"},
                  "test_results": {"total": 1, "passed": 1}
                }
                """;

            WebhookEvent event = buildEvent(payload);
            when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                TestRun tr = inv.getArgument(0);
                tr.setId(UUID.randomUUID());
                return tr;
            });

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

            assertNotNull(result);
            assertEquals("develop", result.getBranch());
        }

        @Test
        void shouldExtractFromGitHubRefWithoutPrefix() {
            String payload = """
                {
                  "ref": "main",
                  "head_commit": {"id": "abc"},
                  "test_results": {"total": 1, "passed": 1}
                }
                """;

            WebhookEvent event = buildEvent(payload);
            when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                TestRun tr = inv.getArgument(0);
                tr.setId(UUID.randomUUID());
                return tr;
            });

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

            assertNotNull(result);
            assertEquals("main", result.getBranch());
        }

        @Test
        void shouldExtractFromGitLabObjectAttributes() {
            String payload = """
                {
                  "object_attributes": {"ref": "release/v2.0"},
                  "checkout_sha": "abc123",
                  "test_results": {"total": 1, "passed": 1}
                }
                """;

            WebhookEvent event = buildEvent(payload);
            when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                TestRun tr = inv.getArgument(0);
                tr.setId(UUID.randomUUID());
                return tr;
            });

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

            assertNotNull(result);
            assertEquals("release/v2.0", result.getBranch());
        }

        @Test
        void shouldExtractFromGenericBranch() {
            String payload = """
                {
                  "branch": "hotfix/fix-123",
                  "test_results": {"total": 1, "passed": 1}
                }
                """;

            WebhookEvent event = buildEvent(payload);
            when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                TestRun tr = inv.getArgument(0);
                tr.setId(UUID.randomUUID());
                return tr;
            });

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

            assertNotNull(result);
            assertEquals("hotfix/fix-123", result.getBranch());
        }

        @Test
        void shouldExtractFromTestResultsBranch() {
            String payload = """
                {
                  "test_results": {"total": 1, "passed": 1, "branch": "test-branch"}
                }
                """;

            WebhookEvent event = buildEvent(payload);
            when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                TestRun tr = inv.getArgument(0);
                tr.setId(UUID.randomUUID());
                return tr;
            });

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

            assertNotNull(result);
            assertEquals("test-branch", result.getBranch());
        }

        @Test
        void shouldReturnNullWhenNoBranchPresent() {
            String payload = """
                {
                  "test_results": {"total": 1, "passed": 1}
                }
                """;

            WebhookEvent event = buildEvent(payload);
            when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                TestRun tr = inv.getArgument(0);
                tr.setId(UUID.randomUUID());
                return tr;
            });

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

            assertNotNull(result);
            assertNull(result.getBranch());
        }
    }

    @Nested
    class DurationParsing {

        @Test
        void shouldFallBackToDurationFieldWhenDurationMsMissing() {
            String payload = """
                {
                  "test_results": {
                    "suites": [
                      {
                        "name": "Suite",
                        "tests": [
                          {"name": "test1", "status": "passed", "duration": 500}
                        ]
                      }
                    ]
                  }
                }
                """;

            WebhookEvent event = buildEvent(payload);
            when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
                TestRun tr = inv.getArgument(0);
                tr.setId(UUID.randomUUID());
                return tr;
            });

            TestRun result = creator.createFromPayload(event, TestRun.TestSource.JUNIT);

            assertNotNull(result);
            assertEquals(500L, result.getTestCases().get(0).getDurationMs());
            assertEquals(500L, result.getDurationMs());
        }
    }

    @Nested
    class ProjectAssignment {

        @Test
        void shouldAssignProjectFromIntegrationConfig() {
            String payload = """
                {
                  "test_results": {"total": 5, "passed": 5}
                }
                """;

            WebhookEvent event = buildEvent(payload);
            ArgumentCaptor<TestRun> captor = ArgumentCaptor.forClass(TestRun.class);
            when(testRunRepository.save(captor.capture())).thenAnswer(inv -> {
                TestRun tr = inv.getArgument(0);
                tr.setId(UUID.randomUUID());
                return tr;
            });

            creator.createFromPayload(event, TestRun.TestSource.PLAYWRIGHT);

            TestRun saved = captor.getValue();
            assertNotNull(saved.getProject());
            assertEquals("Test Project", saved.getProject().getName());
        }
    }
}
