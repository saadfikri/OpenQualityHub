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
class GitHubWorkflowProcessorTest {

    @Mock
    private WebhookTestRunCreator testRunCreator;

    @InjectMocks
    private GitHubWorkflowProcessor processor;

    @Test
    void getProvider_shouldReturnGitHub() {
        assertEquals(IntegrationConfig.Provider.GITHUB, processor.getProvider());
    }

    @Test
    void supports_shouldAcceptWorkflowRun() {
        assertTrue(processor.supports("workflow_run"));
    }

    @Test
    void supports_shouldAcceptCheckSuite() {
        assertTrue(processor.supports("check_suite"));
    }

    @Test
    void supports_shouldRejectPush() {
        assertFalse(processor.supports("push"));
    }

    @Test
    void supports_shouldRejectNull() {
        assertFalse(processor.supports(null));
    }

    @Test
    void process_shouldNotThrowOnValidEvent() {
        WebhookEvent event = WebhookEvent.builder()
            .eventType("workflow_run")
            .provider(IntegrationConfig.Provider.GITHUB)
            .payload("{\"action\":\"completed\",\"workflow_run\":{\"conclusion\":\"success\"}}")
            .build();
        event.setId(UUID.randomUUID());

        assertDoesNotThrow(() -> processor.process(event));
    }

    @Test
    void process_shouldNotThrowOnMalformedPayload() {
        WebhookEvent event = WebhookEvent.builder()
            .eventType("workflow_run")
            .provider(IntegrationConfig.Provider.GITHUB)
            .payload("{invalid json")
            .build();
        event.setId(UUID.randomUUID());

        assertDoesNotThrow(() -> processor.process(event));
    }
}
