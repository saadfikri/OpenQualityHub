package com.openqualityhub.dto.response;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class TeamResponse {
    private UUID id;
    private String name;
    private String description;
    private int projectCount;
    private Instant createdAt;
    private Instant updatedAt;
}
