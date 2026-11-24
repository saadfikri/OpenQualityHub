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
public class AuditEventResponse {

    private UUID id;
    private String entityType;
    private UUID entityId;
    private String action;
    private String details;
    private String userName;
    private String ipAddress;
    private Instant createdAt;
}
