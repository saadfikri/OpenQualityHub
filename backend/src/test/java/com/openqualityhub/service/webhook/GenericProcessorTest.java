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
class GenericProcessorTest {

    @Mock
    private WebhookTestRunCreator testRunCreator;

    @InjectMocks
    private GenericProcessor processor;

    @Test
    void getProvider_shouldReturnGeneric() {
        assertEquals(IntegrationConfig.Provider.GENERIC, processor.getProvider());
    }

    @Test
    void supports_shouldAcceptAnyEventType() {
        assertTrue(processor.supports("anything"));
        assertTrue(processor.supports("custom_event"));
        assertTrue(processor.supports("deployment"));
    }

    @Test
    void supports_shouldAcceptNull() {
        assertTrue(processor.supports(null));
    }

    @Test
    void supports_shouldAcceptEmptyString() {
        assertTrue(processor.supports(""));
    }

    @Test
    void process_shouldNotThrowOnValidEvent() {
        WebhookEvent event = WebhookEvent.builder()
            .eventType("custom_event")
            .provider(IntegrationConfig.Provider.GENERIC)
            .payload("{\"key\":\"value\"}")
            .build();
        event.setId(UUID.randomUUID());

        assertDoesNotThrow(() -> processor.process(event));
    }

    @Test
    void process_shouldNotThrowOnMalformedPayload() {
        WebhookEvent event = WebhookEvent.builder()
            .eventType("custom_event")
            .provider(IntegrationConfig.Provider.GENERIC)
            .payload("this is not json at all")
            .build();
        event.setId(UUID.randomUUID());

        assertDoesNotThrow(() -> processor.process(event));
    }
}
