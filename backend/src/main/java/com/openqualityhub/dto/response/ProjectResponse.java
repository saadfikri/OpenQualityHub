package com.openqualityhub.dto.response;

import com.openqualityhub.entity.Project.ProjectStatus;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class ProjectResponse {
    private UUID id;
    private String name;
    private String description;
    private ProjectStatus status;
    private UUID teamId;
    private String teamName;
    private int testRunCount;
    private Instant createdAt;
    private Instant updatedAt;
}
