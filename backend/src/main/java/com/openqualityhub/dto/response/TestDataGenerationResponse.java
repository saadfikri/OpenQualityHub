package com.openqualityhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestDataGenerationResponse {

    private UUID id;
    private UUID templateId;
    private String templateName;
    private UUID profileId;
    private String profileName;
    private int rowCount;
    private String status;
    private String exportFormat;
    private String errorMessage;
    private Instant startedAt;
    private Instant completedAt;
    private Instant createdAt;
}
