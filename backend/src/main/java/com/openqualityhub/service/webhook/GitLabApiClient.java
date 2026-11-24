package com.openqualityhub.service.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Fetches test reports from GitLab API.
 * Uses the Pipeline Test Report endpoint:
 * GET /api/v4/projects/:id/pipelines/:pipeline_id/test_report
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GitLabApiClient {

    private final ObjectMapper objectMapper;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Fetch test report for a pipeline.
     * @param gitlabUrl base URL (e.g. "https://gitlab.com")
     * @param apiToken  GitLab personal/project access token
     * @param projectId GitLab project ID (numeric)
     * @param pipelineId GitLab pipeline ID (numeric)
     * @return parsed JSON test report, or null if unavailable
     */
    public JsonNode fetchTestReport(String gitlabUrl, String apiToken, long projectId, long pipelineId) {
        String url = String.format("%s/api/v4/projects/%d/pipelines/%d/test_report",
                gitlabUrl.replaceAll("/+$", ""), projectId, pipelineId);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("PRIVATE-TOKEN", apiToken)
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode report = objectMapper.readTree(response.body());
                log.info("Fetched GitLab test report for project {} pipeline {}: {} total, {} success, {} failed",
                        projectId, pipelineId,
                        report.path("total_count").asInt(),
                        report.path("success_count").asInt(),
                        report.path("failed_count").asInt());
                return report;
            } else if (response.statusCode() == 404) {
                log.info("No test report found for project {} pipeline {} (404)", projectId, pipelineId);
                return null;
            } else {
                log.warn("GitLab API returned {} for test report (project={}, pipeline={}): {}",
                        response.statusCode(), projectId, pipelineId, response.body());
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to fetch GitLab test report for project {} pipeline {}: {}",
                    projectId, pipelineId, e.getMessage());
            return null;
        }
    }

    /**
     * Fetch pipeline details (for branch, sha, etc. if not in webhook payload).
     */
    public JsonNode fetchPipeline(String gitlabUrl, String apiToken, long projectId, long pipelineId) {
        String url = String.format("%s/api/v4/projects/%d/pipelines/%d",
                gitlabUrl.replaceAll("/+$", ""), projectId, pipelineId);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("PRIVATE-TOKEN", apiToken)
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readTree(response.body());
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch GitLab pipeline details: {}", e.getMessage());
            return null;
        }
    }
}
