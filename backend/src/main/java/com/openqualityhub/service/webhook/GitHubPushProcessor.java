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
public class GitHubPushProcessor implements WebhookEventProcessor {

    private final WebhookTestRunCreator testRunCreator;

    @Override
    public IntegrationConfig.Provider getProvider() {
        return IntegrationConfig.Provider.GITHUB;
    }

    @Override
    public boolean supports(String eventType) {
        return "push".equals(eventType);
    }

    @Override
    public void process(WebhookEvent event) {
        log.info("Processing GitHub push event: {} ({})", event.getEventType(), event.getId());
        TestRun testRun = testRunCreator.createFromPayload(event, TestRun.TestSource.JUNIT);
        if (testRun != null) {
            log.info("Created test run {} from GitHub push event", testRun.getId());
        }
    }
}
