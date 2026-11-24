package com.openqualityhub.service.webhook;

import com.openqualityhub.entity.IntegrationConfig;
import com.openqualityhub.entity.WebhookEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GitHubIssueProcessorTest {

    private final GitHubIssueProcessor processor = new GitHubIssueProcessor();

    @Test
    void getProvider_shouldReturnGitHub() {
        assertEquals(IntegrationConfig.Provider.GITHUB, processor.getProvider());
    }

    @Test
    void supports_shouldAcceptIssues() {
        assertTrue(processor.supports("issues"));
    }

    @Test
    void supports_shouldRejectPullRequest() {
        assertFalse(processor.supports("pull_request"));
    }

    @Test
    void supports_shouldRejectNull() {
        assertFalse(processor.supports(null));
    }

    @Test
    void process_shouldNotThrowOnValidEvent() {
        WebhookEvent event = WebhookEvent.builder()
            .eventType("issues")
            .provider(IntegrationConfig.Provider.GITHUB)
            .payload("{\"action\":\"opened\",\"issue\":{\"title\":\"Bug found\",\"number\":42}}")
            .build();
        event.setId(UUID.randomUUID());

        assertDoesNotThrow(() -> processor.process(event));
    }

    @Test
    void process_shouldNotThrowOnEmptyPayload() {
        WebhookEvent event = WebhookEvent.builder()
            .eventType("issues")
            .provider(IntegrationConfig.Provider.GITHUB)
            .payload("{}")
            .build();
        event.setId(UUID.randomUUID());

        assertDoesNotThrow(() -> processor.process(event));
    }
}
