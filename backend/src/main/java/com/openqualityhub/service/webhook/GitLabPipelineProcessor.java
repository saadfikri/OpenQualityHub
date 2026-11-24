package com.openqualityhub.service.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openqualityhub.entity.*;
import com.openqualityhub.repository.TestRunRepository;
import com.openqualityhub.service.ContributorService;
import com.openqualityhub.service.impl.TestIdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Processes GitLab Pipeline Hook and Job Hook events.
 *
 * Strategy:
 * 1. If the webhook payload contains "test_results" (custom CI script), use WebhookTestRunCreator
 * 2. Otherwise, fetch test report from GitLab API /pipelines/:id/test_report
 * 3. Parse test_suites → test_cases into OQH TestRun + TestCase entities
 *
 * Requires integration config JSONB: {"gitlab_url": "https://gitlab.com", "api_token": "glpat-xxx"}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GitLabPipelineProcessor implements WebhookEventProcessor {

    private final WebhookTestRunCreator testRunCreator;
    private final GitLabApiClient gitLabApiClient;
    private final ObjectMapper objectMapper;
    private final TestRunRepository testRunRepository;
    private final TestIdentityService testIdentityService;
    private final ContributorService contributorService;

    @Override
    public IntegrationConfig.Provider getProvider() {
        return IntegrationConfig.Provider.GITLAB;
    }

    @Override
    public boolean supports(String eventType) {
        return "Pipeline Hook".equals(eventType) || "Job Hook".equals(eventType);
    }

    @Override
    @Transactional
    public void process(WebhookEvent event) {
        log.info("Processing GitLab event: {} ({})", event.getEventType(), event.getId());

        try {
            JsonNode root = objectMapper.readTree(event.getPayload());

            // Track contributor from webhook payload
            captureContributor(event, root);

            // Strategy 1: Custom payload with test_results (from CI script calling our webhook)
            if (root.has("test_results")) {
                TestRun testRun = testRunCreator.createFromPayload(event, TestRun.TestSource.JUNIT);
                if (testRun != null) {
                    log.info("Created test run {} from GitLab custom payload", testRun.getId());
                    return;
                }
            }

            // Strategy 2: Fetch from GitLab API
            IntegrationConfig config = event.getIntegration();
            JsonNode configJson = objectMapper.readTree(
                    config.getConfig() != null ? config.getConfig() : "{}");

            String gitlabUrl = configJson.path("gitlab_url").asText("");
            String apiToken = configJson.path("api_token").asText("");

            if (gitlabUrl.isBlank() || apiToken.isBlank()) {
                log.info("No GitLab API credentials configured for integration {}, skipping test report fetch", config.getId());
                return;
            }

            // Extract pipeline info from webhook payload
            long gitlabProjectId = extractGitLabProjectId(root);
            long pipelineId = extractPipelineId(root);

            if (gitlabProjectId <= 0 || pipelineId <= 0) {
                log.warn("Could not extract GitLab project/pipeline ID from webhook payload");
                return;
            }

            // Only process completed pipelines
            String pipelineStatus = extractPipelineStatus(root);
            if (!"success".equals(pipelineStatus) && !"failed".equals(pipelineStatus)) {
                log.info("Pipeline {} status is '{}', skipping (only processing success/failed)", pipelineId, pipelineStatus);
                return;
            }

            // Fetch test report from GitLab API
            JsonNode testReport = gitLabApiClient.fetchTestReport(gitlabUrl, apiToken, gitlabProjectId, pipelineId);
            if (testReport == null || testReport.path("total_count").asInt(0) == 0) {
                log.info("No test report or 0 tests for pipeline {}", pipelineId);
                return;
            }

            // Build TestRun from the test report
            Project project = config.getProject();
            if (project == null) {
                log.warn("Integration {} has no linked project", config.getId());
                return;
            }

            String branch = extractBranch(root);
            String commitSha = extractCommitSha(root);
            String pipelineRef = root.path("object_attributes").path("ref").asText(
                    root.path("ref").asText("unknown"));

            TestRun testRun = TestRun.builder()
                    .name("Pipeline #" + pipelineId + " — " + pipelineRef)
                    .source(TestRun.TestSource.JUNIT)
                    .project(project)
                    .branch(branch)
                    .commitSha(commitSha)
                    .build();

            List<TestCase> testCases = new ArrayList<>();
            int totalTests = 0, passed = 0, failed = 0, skipped = 0;
            long totalDuration = 0;

            // Parse test_suites from GitLab test report
            JsonNode testSuites = testReport.path("test_suites");
            for (JsonNode suite : testSuites) {
                String suiteName = suite.path("name").asText("Default Suite");

                JsonNode cases = suite.path("test_cases");
                for (JsonNode tc : cases) {
                    String name = tc.path("name").asText("Unknown");
                    String className = tc.path("classname").asText(null);
                    String statusStr = tc.path("status").asText("success");
                    double executionTime = tc.path("execution_time").asDouble(0);
                    long durationMs = Math.round(executionTime * 1000);

                    TestCase.TestStatus status = switch (statusStr.toLowerCase()) {
                        case "success" -> TestCase.TestStatus.PASSED;
                        case "failed" -> TestCase.TestStatus.FAILED;
                        case "skipped" -> TestCase.TestStatus.SKIPPED;
                        case "error" -> TestCase.TestStatus.ERROR;
                        default -> TestCase.TestStatus.FAILED;
                    };

                    String errorMessage = null;
                    String stackTrace = null;
                    if (tc.has("system_output") && !tc.path("system_output").isNull()) {
                        String output = tc.path("system_output").asText("");
                        if (!output.isBlank()) {
                            // First line = error message, rest = stack trace
                            int newline = output.indexOf('\n');
                            if (newline > 0) {
                                errorMessage = output.substring(0, Math.min(newline, 500));
                                stackTrace = output;
                            } else {
                                errorMessage = output.substring(0, Math.min(output.length(), 500));
                            }
                        }
                    }

                    testCases.add(TestCase.builder()
                            .name(name)
                            .className(className)
                            .suiteName(suiteName)
                            .status(status)
                            .durationMs(durationMs)
                            .errorMessage(errorMessage)
                            .stackTrace(stackTrace)
                            .testRun(testRun)
                            .build());

                    totalTests++;
                    totalDuration += durationMs;
                    switch (status) {
                        case PASSED -> passed++;
                        case FAILED, ERROR -> failed++;
                        case SKIPPED -> skipped++;
                    }
                }
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

            log.info("Created test run {} from GitLab pipeline #{}: {} tests ({} pass, {} fail, {} skip) for project {}",
                    testRun.getId(), pipelineId, totalTests, passed, failed, skipped, project.getName());

        } catch (Exception e) {
            log.error("Error processing GitLab event {}: {}", event.getId(), e.getMessage(), e);
        }
    }

    private long extractGitLabProjectId(JsonNode root) {
        // Pipeline Hook: project.id
        if (root.has("project") && root.path("project").has("id")) {
            return root.path("project").path("id").asLong(0);
        }
        // Job Hook: project_id
        return root.path("project_id").asLong(0);
    }

    private long extractPipelineId(JsonNode root) {
        // Pipeline Hook: object_attributes.id
        if (root.has("object_attributes")) {
            return root.path("object_attributes").path("id").asLong(0);
        }
        // Job Hook: pipeline.id
        if (root.has("pipeline")) {
            return root.path("pipeline").path("id").asLong(0);
        }
        return 0;
    }

    private String extractPipelineStatus(JsonNode root) {
        if (root.has("object_attributes")) {
            return root.path("object_attributes").path("status").asText("");
        }
        if (root.has("pipeline")) {
            return root.path("pipeline").path("status").asText("");
        }
        return root.path("status").asText("");
    }

    private String extractBranch(JsonNode root) {
        if (root.has("object_attributes")) {
            return root.path("object_attributes").path("ref").asText(null);
        }
        return root.path("ref").asText(null);
    }

    private String extractCommitSha(JsonNode root) {
        if (root.has("object_attributes")) {
            return root.path("object_attributes").path("sha").asText(null);
        }
        if (root.has("checkout_sha")) {
            return root.path("checkout_sha").asText(null);
        }
        return root.path("sha").asText(null);
    }

    private void captureContributor(WebhookEvent event, JsonNode root) {
        try {
            JsonNode userNode = root.path("user");
            if (userNode.isMissingNode() || !userNode.has("id")) return;

            String externalId = String.valueOf(userNode.path("id").asLong());
            String username = userNode.path("username").asText(null);
            String displayName = userNode.path("name").asText(null);
            String email = userNode.path("email").asText(null);
            String avatarUrl = userNode.path("avatar_url").asText(null);

            if (username == null) return;

            Organization org = event.getIntegration().getOrganization();
            Project project = event.getIntegration().getProject();

            Contributor contributor = contributorService.upsertFromWebhook(
                    org, Contributor.Provider.GITLAB, externalId,
                    username, displayName, email, avatarUrl);

            String branch = extractBranch(root);
            String commitSha = extractCommitSha(root);
            String pipelineStatus = extractPipelineStatus(root);
            long pipelineId = extractPipelineId(root);

            // Record pipeline trigger activity
            contributorService.recordActivity(contributor,
                    ContributorActivity.ActivityType.PIPELINE_TRIGGER,
                    project, branch, commitSha,
                    root.path("commit").path("message").asText(null),
                    pipelineId > 0 ? String.valueOf(pipelineId) : null,
                    pipelineStatus);

            log.debug("Tracked contributor {} for pipeline {}", username, pipelineId);
        } catch (Exception e) {
            log.warn("Failed to capture contributor from webhook: {}", e.getMessage());
        }
    }
}
