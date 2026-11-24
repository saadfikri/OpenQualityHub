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
class JenkinsProcessorTest {

    @Mock
    private WebhookTestRunCreator testRunCreator;

    @InjectMocks
    private JenkinsProcessor processor;

    @Test
    void getProvider_shouldReturnJenkins() {
        assertEquals(IntegrationConfig.Provider.JENKINS, processor.getProvider());
    }

    @Test
    void supports_shouldAcceptBuildCompleted() {
        assertTrue(processor.supports("build_completed"));
    }

    @Test
    void supports_shouldAcceptBuildStarted() {
        assertTrue(processor.supports("build_started"));
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
            .eventType("build_completed")
            .provider(IntegrationConfig.Provider.JENKINS)
            .payload("{\"build\":{\"status\":\"SUCCESS\",\"number\":101}}")
            .build();
        event.setId(UUID.randomUUID());

        assertDoesNotThrow(() -> processor.process(event));
    }

    @Test
    void process_shouldNotThrowOnMalformedPayload() {
        WebhookEvent event = WebhookEvent.builder()
            .eventType("build_completed")
            .provider(IntegrationConfig.Provider.JENKINS)
            .payload("not valid json")
            .build();
        event.setId(UUID.randomUUID());

        assertDoesNotThrow(() -> processor.process(event));
    }
}
