package com.openqualityhub.service.webhook;

import com.openqualityhub.entity.IntegrationConfig;
import com.openqualityhub.entity.WebhookEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JiraIssueProcessor implements WebhookEventProcessor {

    @Override
    public IntegrationConfig.Provider getProvider() {
        return IntegrationConfig.Provider.JIRA;
    }

    @Override
    public boolean supports(String eventType) {
        return eventType != null && eventType.startsWith("jira:issue_");
    }

    @Override
    public void process(WebhookEvent event) {
        log.info("Processing Jira issue event: {} ({})", event.getEventType(), event.getId());
        // Future: parse payload to create/update Defect records
    }
}
