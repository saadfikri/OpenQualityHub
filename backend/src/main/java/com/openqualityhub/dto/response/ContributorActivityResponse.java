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
public class ContributorActivityResponse {

    private UUID id;
    private UUID contributorId;
    private String activityType;
    private UUID projectId;
    private String projectName;
    private String branch;
    private String commitSha;
    private String commitMessage;
    private String pipelineId;
    private String pipelineStatus;
    private Instant createdAt;
}
