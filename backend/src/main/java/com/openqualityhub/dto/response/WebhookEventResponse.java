package com.openqualityhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEventResponse {

    private UUID id;
    private UUID integrationId;
    private String eventType;
    private String provider;
    private String status;
    private String processingError;
    private Instant processedAt;
    private Instant receivedAt;
    private String payload;
}
