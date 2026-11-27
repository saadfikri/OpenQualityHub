package com.openqualityhub.service.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openqualityhub.entity.*;
import com.openqualityhub.repository.MaestroFlowResultRepository;
import com.openqualityhub.repository.TestRunRepository;
import com.openqualityhub.service.impl.TestIdentityService;
import com.openqualityhub.service.impl.parser.MaestroJsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class MaestroCloudProcessor implements WebhookEventProcessor {

    private final ObjectMapper objectMapper;
    private final MaestroJsonParser maestroParser;
    private final TestRunRepository testRunRepository;
    private final MaestroFlowResultRepository flowResultRepository;
    private final TestIdentityService testIdentityService;

    @Override
    public IntegrationConfig.Provider getProvider() {
        return IntegrationConfig.Provider.MAESTRO;
    }

    @Override
    public boolean supports(String eventType) {
        return "flow_complete".equals(eventType)
            || "suite_complete".equals(eventType)
            || "test_complete".equals(eventType)
            || "upload_complete".equals(eventType);
    }

    @Override
    @Transactional
    public void process(WebhookEvent event) {
        log.info("Processing Maestro Cloud event: {} ({})", event.getEventType(), event.getId());
        try {
            JsonNode root = objectMapper.readTree(event.getPayload());

            IntegrationConfig config = event.getIntegration();
            Project project = config.getProject();
            if (project == null) {
                log.warn("Maestro integration {} has no linked project, skipping", config.getId());
                return;
            }

            // Parse Maestro results using the parser
            MaestroJsonParser.ParseResult result = maestroParser.parse(
                new ByteArrayInputStream(event.getPayload().getBytes(StandardCharsets.UTF_8)));

            TestRun testRun = result.testRun();
            testRun.setProject(project);
            testRun.setSource(TestRun.TestSource.MAESTRO);
            testRun.setName(extractRunName(root, event));
            testRun.setEnvironment(root.path("environment").asText(null));
            testRun.setCommitSha(root.path("commit_sha").asText(root.path("git").path("sha").asText(null)));
            testRun.setBranch(root.path("branch").asText(root.path("git").path("branch").asText(null)));

            testRun = testRunRepository.save(testRun);

            // Save flow results with enriched device data
            if (!result.flowResults().isEmpty()) {
                for (MaestroFlowResult fr : result.flowResults()) {
                    fr.setTestRun(testRun);
                }
                flowResultRepository.saveAll(result.flowResults());
            }

            if (!testRun.getTestCases().isEmpty()) {
                testIdentityService.recordExecutions(testRun);
            }

            log.info("Created Maestro test run {} ({} flows, {} devices) for project {}",
                testRun.getId(), testRun.getTotalTests(),
                result.flowResults().stream()
                    .map(MaestroFlowResult::getDeviceName)
                    .distinct().count(),
                project.getName());
        } catch (Exception e) {
            log.error("Failed to process Maestro Cloud event: {}", e.getMessage(), e);
            throw new RuntimeException("Maestro webhook processing failed", e);
        }
    }

    private String extractRunName(JsonNode root, WebhookEvent event) {
        if (root.has("name")) return root.path("name").asText();
        if (root.has("suite_name")) return root.path("suite_name").asText();
        return "Maestro Run #" + event.getId().toString().substring(0, 8);
    }
}
