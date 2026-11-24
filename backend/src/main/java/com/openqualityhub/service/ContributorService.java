package com.openqualityhub.service;

import com.openqualityhub.dto.response.ContributorActivityResponse;
import com.openqualityhub.dto.response.ContributorDetailResponse;
import com.openqualityhub.dto.response.ContributorResponse;
import com.openqualityhub.entity.Contributor;
import com.openqualityhub.entity.ContributorActivity;
import com.openqualityhub.entity.Organization;
import com.openqualityhub.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ContributorService {

    Page<ContributorResponse> getAll(Pageable pageable);

    Page<ContributorResponse> search(String query, Pageable pageable);

    ContributorDetailResponse getById(UUID id);

    Page<ContributorActivityResponse> getActivities(UUID contributorId, Pageable pageable);

    Page<ContributorActivityResponse> getActivitiesByProject(UUID projectId, Pageable pageable);

    Contributor upsertFromWebhook(Organization org, Contributor.Provider provider, String externalId,
                                   String username, String displayName, String email, String avatarUrl);

    ContributorActivity recordActivity(Contributor contributor, ContributorActivity.ActivityType type,
                                        Project project, String branch, String commitSha,
                                        String commitMessage, String pipelineId, String pipelineStatus);

    ContributorResponse linkUser(UUID contributorId, UUID userId);

    ContributorResponse unlinkUser(UUID contributorId);
}
