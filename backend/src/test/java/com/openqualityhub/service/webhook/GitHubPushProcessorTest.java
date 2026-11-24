package com.openqualityhub.service.webhook;

import com.openqualityhub.entity.IntegrationConfig;
import com.openqualityhub.entity.WebhookEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GitHubPushProcessorTest {

    @Mock
    private WebhookTestRunCreator testRunCreator;

    @InjectMocks
    private GitHubPushProcessor processor;

    @Test
    void getProvider_shouldReturnGitHub() {
        assertEquals(IntegrationConfig.Provider.GITHUB, processor.getProvider());
    }

    @Test
    void supports_shouldAcceptPush() {
        assertTrue(processor.supports("push"));
    }

    @Test
    void supports_shouldRejectIssues() {
        assertFalse(processor.supports("issues"));
    }

    @Test
    void supports_shouldRejectNull() {
        assertFalse(processor.supports(null));
    }

    @Test
    void process_shouldNotThrowOnValidEvent() {
        WebhookEvent event = WebhookEvent.builder()
            .eventType("push")
            .provider(IntegrationConfig.Provider.GITHUB)
            .payload("{\"ref\":\"refs/heads/main\",\"commits\":[{\"id\":\"abc123\"}]}")
            .build();
        event.setId(UUID.randomUUID());

        assertDoesNotThrow(() -> processor.process(event));
    }
}
