package com.openqualityhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseResponse {
    private UUID id;
    private String releaseVersion;
    private String name;
    private String description;
    private String status;
    private LocalDate targetDate;
    private LocalDate actualDate;
    private UUID projectId;
    private String projectName;
    private Instant createdAt;
    private Instant updatedAt;
}
