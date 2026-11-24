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
public class TestDataTemplateResponse {

    private UUID id;
    private String name;
    private String description;
    private UUID projectId;
    private String projectName;
    private int fieldCount;
    private int profileCount;
    private int generationCount;
    private Instant createdAt;
    private Instant updatedAt;
}
