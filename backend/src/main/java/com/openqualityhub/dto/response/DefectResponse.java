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
public class DefectResponse {
    private UUID id;
    private String title;
    private String description;
    private String severity;
    private String status;
    private int priority;
    private String component;
    private UUID projectId;
    private String projectName;
    private UUID releaseId;
    private String releaseName;
    private UUID assignedToId;
    private String assignedToName;
    private UUID reportedById;
    private String reportedByName;
    private String resolution;
    private Instant resolvedAt;
    private String externalId;
    private String externalUrl;
    private Instant createdAt;
    private Instant updatedAt;
}
