package com.openqualityhub.service.webhook;

import com.openqualityhub.entity.IntegrationConfig;
import com.openqualityhub.entity.TestRun;
import com.openqualityhub.entity.WebhookEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JenkinsProcessor implements WebhookEventProcessor {

    private final WebhookTestRunCreator testRunCreator;

    @Override
    public IntegrationConfig.Provider getProvider() {
        return IntegrationConfig.Provider.JENKINS;
    }

    @Override
    public boolean supports(String eventType) {
        return "build_completed".equals(eventType) || "build_started".equals(eventType);
    }

    @Override
    public void process(WebhookEvent event) {
        log.info("Processing Jenkins build event: {} ({})", event.getEventType(), event.getId());
        TestRun testRun = testRunCreator.createFromPayload(event, TestRun.TestSource.JUNIT);
        if (testRun != null) {
            log.info("Created test run {} from Jenkins build event", testRun.getId());
        }
    }
}
