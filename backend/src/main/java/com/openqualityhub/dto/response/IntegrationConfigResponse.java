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
public class IntegrationConfigResponse {

    private UUID id;
    private String name;
    private String description;
    private String provider;
    private boolean enabled;
    private boolean hasSecret;
    private UUID projectId;
    private String projectName;
    private long eventCount;
    private Instant lastEventAt;
    private Instant createdAt;
    private Instant updatedAt;
}
