package com.openqualityhub.service;

import java.util.Map;
import java.util.UUID;

public interface WebhookService {

    void receive(UUID integrationId, String payload, Map<String, String> headers);
}
