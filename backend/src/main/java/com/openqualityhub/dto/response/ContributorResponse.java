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
public class ContributorResponse {

    private UUID id;
    private String externalId;
    private String provider;
    private String username;
    private String displayName;
    private String email;
    private String avatarUrl;
    private int totalCommits;
    private int totalPipelines;
    private int totalMerges;
    private Instant lastActivityAt;
    private UUID linkedUserId;
    private String linkedUserName;
    private Instant createdAt;
}
