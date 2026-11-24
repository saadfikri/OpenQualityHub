package com.openqualityhub.service.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openqualityhub.entity.*;
import com.openqualityhub.repository.ProjectRepository;
import com.openqualityhub.repository.TestRunRepository;
import com.openqualityhub.service.impl.TestIdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookTestRunCreator {

    private final ObjectMapper objectMapper;
    private final TestRunRepository testRunRepository;
    private final ProjectRepository projectRepository;
    private final TestIdentityService testIdentityService;

    /**
     * Parse webhook payload's test_results and create a TestRun for the integration's project.
     * Returns the created TestRun, or null if no test_results in payload.
     */
    @Transactional
    public TestRun createFromPayload(WebhookEvent event, TestRun.TestSource source) {
        try {
            JsonNode root = objectMapper.readTree(event.getPayload());
            JsonNode testResults = root.path("test_results");
            if (testResults.isMissingNode() || testResults.isNull()) {
                return null;
            }

            IntegrationConfig config = event.getIntegration();
            Project project = config.getProject();
            if (project == null) {
                log.warn("Integration {} has no linked project, skipping test run creation", config.getId());
                return null;
            }

            String runName = testResults.path("name").asText("Webhook Import #" + event.getId().toString().substring(0, 8));

            TestRun testRun = TestRun.builder()
                .name(runName)
                .source(source)
                .project(project)
                .environment(testResults.path("environment").asText(null))
                .commitSha(extractCommitSha(root))
                .branch(extractBranch(root))
                .build();

            List<TestCase> testCases = new ArrayList<>();
            int totalTests = 0, passed = 0, failed = 0, skipped = 0;
            long totalDuration = 0;

            // Parse individual test cases from suites if available
            JsonNode suites = testResults.path("suites");
            if (suites.isArray() && suites.size() > 0) {
                for (JsonNode suite : suites) {
                    String suiteName = suite.path("name").asText("Default Suite");
                    JsonNode tests = suite.path("tests");
                    for (JsonNode test : tests) {
                        String name = test.path("name").asText("Unknown Test");
                        String statusStr = test.path("status").asText("passed");
                        long duration = test.path("duration_ms").asLong(test.path("duration").asLong(0));

                        TestCase.TestStatus status = switch (statusStr.toLowerCase()) {
                            case "passed", "pass", "success" -> TestCase.TestStatus.PASSED;
                            case "failed", "fail", "failure" -> TestCase.TestStatus.FAILED;
                            case "skipped", "skip", "pending", "disabled" -> TestCase.TestStatus.SKIPPED;
                            case "error", "broken", "timedout" -> TestCase.TestStatus.ERROR;
                            default -> TestCase.TestStatus.FAILED;
                        };

                        String errorMsg = test.has("error")
                            ? (test.path("error").isTextual() ? test.path("error").asText() : test.path("error").path("message").asText(null))
                            : null;
                        String stackTrace = test.has("error") && test.path("error").isObject()
                            ? test.path("error").path("stack").asText(null)
                            : null;

                        TestCase tc = TestCase.builder()
                            .name(name)
                            .suiteName(suiteName)
                            .status(status)
                            .durationMs(duration)
                            .errorMessage(errorMsg)
                            .stackTrace(stackTrace)
                            .testRun(testRun)
                            .build();
                        testCases.add(tc);

                        totalTests++;
                        totalDuration += duration;
                        switch (status) {
                            case PASSED -> passed++;
                            case FAILED, ERROR -> failed++;
                            case SKIPPED -> skipped++;
                        }
                    }
                }
            } else {
                // Summary-only mode (no individual test cases)
                totalTests = testResults.path("total").asInt(0);
                passed = testResults.path("passed").asInt(0);
                failed = testResults.path("failed").asInt(0);
                skipped = testResults.path("skipped").asInt(0);
                totalDuration = testResults.path("duration_ms").asLong(0);
            }

            testRun.setTotalTests(totalTests);
            testRun.setPassed(passed);
            testRun.setFailed(failed);
            testRun.setSkipped(skipped);
            testRun.setDurationMs(totalDuration);
            testRun.setTestCases(testCases);

            testRun = testRunRepository.save(testRun);
            if (!testCases.isEmpty()) {
                testIdentityService.recordExecutions(testRun);
            }

            log.info("Created test run from webhook: {} ({} tests, {}% pass rate) for project {}",
                testRun.getId(), totalTests, totalTests > 0 ? (passed * 100 / totalTests) : 0, project.getName());
            return testRun;
        } catch (Exception e) {
            log.error("Failed to create test run from webhook payload: {}", e.getMessage(), e);
            return null;
        }
    }

    private String extractCommitSha(JsonNode root) {
        // GitHub
        if (root.has("head_commit")) return root.path("head_commit").path("id").asText(null);
        if (root.has("workflow_run")) return root.path("workflow_run").path("head_sha").asText(null);
        // GitLab
        if (root.has("checkout_sha")) return root.path("checkout_sha").asText(null);
        // Generic
        if (root.has("commit_sha")) return root.path("commit_sha").asText(null);
        if (root.has("test_results")) return root.path("test_results").path("commit_sha").asText(null);
        return null;
    }

    private String extractBranch(JsonNode root) {
        // GitHub
        if (root.has("workflow_run")) return root.path("workflow_run").path("head_branch").asText(null);
        if (root.has("ref")) {
            String ref = root.path("ref").asText("");
            return ref.startsWith("refs/heads/") ? ref.substring(11) : ref;
        }
        // GitLab
        if (root.has("object_attributes")) return root.path("object_attributes").path("ref").asText(null);
        // Generic
        if (root.has("branch")) return root.path("branch").asText(null);
        if (root.has("test_results")) return root.path("test_results").path("branch").asText(null);
        return null;
    }
}
