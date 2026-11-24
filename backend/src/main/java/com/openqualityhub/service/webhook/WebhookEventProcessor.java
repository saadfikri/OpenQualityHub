package com.openqualityhub.service.webhook;

import com.openqualityhub.entity.IntegrationConfig;
import com.openqualityhub.entity.WebhookEvent;

public interface WebhookEventProcessor {

    IntegrationConfig.Provider getProvider();

    boolean supports(String eventType);

    void process(WebhookEvent event);
}
