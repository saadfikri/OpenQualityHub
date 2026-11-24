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
public class GenericProcessor implements WebhookEventProcessor {

    private final WebhookTestRunCreator testRunCreator;

    @Override
    public IntegrationConfig.Provider getProvider() {
        return IntegrationConfig.Provider.GENERIC;
    }

    @Override
    public boolean supports(String eventType) {
        return true; // generic accepts all
    }

    @Override
    public void process(WebhookEvent event) {
        log.info("Processing generic webhook event: {} ({})", event.getEventType(), event.getId());
        TestRun testRun = testRunCreator.createFromPayload(event, TestRun.TestSource.JUNIT);
        if (testRun != null) {
            log.info("Created test run {} from generic webhook event", testRun.getId());
        }
    }
}
