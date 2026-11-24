package com.openqualityhub.service.webhook;

import com.openqualityhub.entity.IntegrationConfig;
import com.openqualityhub.entity.WebhookEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JiraIssueProcessorTest {

    private final JiraIssueProcessor processor = new JiraIssueProcessor();

    @Test
    void getProvider_shouldReturnJira() {
        assertEquals(IntegrationConfig.Provider.JIRA, processor.getProvider());
    }

    @Test
    void supports_shouldAcceptJiraIssueCreated() {
        assertTrue(processor.supports("jira:issue_created"));
    }

    @Test
    void supports_shouldAcceptJiraIssueUpdated() {
        assertTrue(processor.supports("jira:issue_updated"));
    }

    @Test
    void supports_shouldAcceptJiraIssueDeleted() {
        assertTrue(processor.supports("jira:issue_deleted"));
    }

    @Test
    void supports_shouldRejectNonJiraEvent() {
        assertFalse(processor.supports("push"));
    }

    @Test
    void supports_shouldRejectNull() {
        assertFalse(processor.supports(null));
    }

    @Test
    void process_shouldNotThrowOnValidEvent() {
        WebhookEvent event = WebhookEvent.builder()
            .eventType("jira:issue_created")
            .provider(IntegrationConfig.Provider.JIRA)
            .payload("{\"issue\":{\"key\":\"PROJ-123\",\"fields\":{\"summary\":\"Bug report\"}}}")
            .build();
        event.setId(UUID.randomUUID());

        assertDoesNotThrow(() -> processor.process(event));
    }
}
