package com.openqualityhub.service.impl;

import com.openqualityhub.dto.response.ContributorActivityResponse;
import com.openqualityhub.dto.response.ContributorDetailResponse;
import com.openqualityhub.dto.response.ContributorResponse;
import com.openqualityhub.entity.Contributor;
import com.openqualityhub.entity.ContributorActivity;
import com.openqualityhub.entity.Organization;
import com.openqualityhub.entity.Project;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.mapper.ContributorMapper;
import com.openqualityhub.repository.ContributorActivityRepository;
import com.openqualityhub.repository.ContributorRepository;
import com.openqualityhub.repository.UserRepository;
import com.openqualityhub.service.ContributorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContributorServiceImpl implements ContributorService {

    private static final UUID DEFAULT_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final ContributorRepository contributorRepository;
    private final ContributorActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final ContributorMapper mapper;

    @Override
    public Page<ContributorResponse> getAll(Pageable pageable) {
        return contributorRepository.findByOrganizationId(DEFAULT_ORG_ID, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public Page<ContributorResponse> search(String query, Pageable pageable) {
        return contributorRepository.searchByUsername(DEFAULT_ORG_ID, query, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public ContributorDetailResponse getById(UUID id) {
        Contributor contributor = contributorRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Contributor", id));

        List<Object[]> typeCounts = activityRepository.countByContributorGroupedByType(id);
        List<ContributorDetailResponse.ActivityBreakdown> breakdown = typeCounts.stream()
                .map(row -> ContributorDetailResponse.ActivityBreakdown.builder()
                        .activityType(((ContributorActivity.ActivityType) row[0]).name())
                        .count((Long) row[1])
                        .build())
                .collect(Collectors.toList());

        List<Object[]> projects = activityRepository.findDistinctProjectsByContributor(id);
        List<ContributorDetailResponse.ProjectBreakdown> projectList = projects.stream()
                .map(row -> ContributorDetailResponse.ProjectBreakdown.builder()
                        .projectId((UUID) row[0])
                        .projectName((String) row[1])
                        .build())
                .collect(Collectors.toList());

        return ContributorDetailResponse.builder()
                .contributor(mapper.toResponse(contributor))
                .activityBreakdown(breakdown)
                .projects(projectList)
                .build();
    }

    @Override
    public Page<ContributorActivityResponse> getActivities(UUID contributorId, Pageable pageable) {
        contributorRepository.findById(contributorId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Contributor", contributorId));
        return activityRepository.findByContributorId(contributorId, pageable)
                .map(mapper::toActivityResponse);
    }

    @Override
    public Page<ContributorActivityResponse> getActivitiesByProject(UUID projectId, Pageable pageable) {
        return activityRepository.findByProjectId(projectId, pageable)
                .map(mapper::toActivityResponse);
    }

    @Override
    @Transactional
    public Contributor upsertFromWebhook(Organization org, Contributor.Provider provider, String externalId,
                                          String username, String displayName, String email, String avatarUrl) {
        return contributorRepository.findByOrgAndProviderAndExternalId(org.getId(), provider, externalId)
                .map(existing -> {
                    if (username != null) existing.setUsername(username);
                    if (displayName != null) existing.setDisplayName(displayName);
                    if (email != null) existing.setEmail(email);
                    if (avatarUrl != null) existing.setAvatarUrl(avatarUrl);
                    return contributorRepository.save(existing);
                })
                .orElseGet(() -> {
                    try {
                        Contributor c = Contributor.builder()
                                .organization(org)
                                .externalId(externalId)
                                .provider(provider)
                                .username(username != null ? username : externalId)
                                .displayName(displayName)
                                .email(email)
                                .avatarUrl(avatarUrl)
                                .build();
                        log.info("New contributor discovered: {} ({}/{})", username, provider, externalId);
                        return contributorRepository.save(c);
                    } catch (DataIntegrityViolationException e) {
                        // Concurrent insert — retry the find
                        return contributorRepository.findByOrgAndProviderAndExternalId(org.getId(), provider, externalId)
                                .orElseThrow(() -> e);
                    }
                });
    }

    @Override
    @Transactional
    public ContributorActivity recordActivity(Contributor contributor, ContributorActivity.ActivityType type,
                                               Project project, String branch, String commitSha,
                                               String commitMessage, String pipelineId, String pipelineStatus) {
        ContributorActivity activity = ContributorActivity.builder()
                .contributor(contributor)
                .activityType(type)
                .project(project)
                .branch(branch)
                .commitSha(commitSha)
                .commitMessage(commitMessage)
                .pipelineId(pipelineId)
                .pipelineStatus(pipelineStatus)
                .build();

        activity = activityRepository.save(activity);

        // Update counters
        switch (type) {
            case COMMIT -> contributor.setTotalCommits(contributor.getTotalCommits() + 1);
            case PIPELINE_TRIGGER -> contributor.setTotalPipelines(contributor.getTotalPipelines() + 1);
            case MERGE -> contributor.setTotalMerges(contributor.getTotalMerges() + 1);
            default -> { }
        }
        contributor.setLastActivityAt(Instant.now());
        contributorRepository.save(contributor);

        return activity;
    }

    @Override
    @Transactional
    public ContributorResponse linkUser(UUID contributorId, UUID userId) {
        Contributor contributor = contributorRepository.findById(contributorId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Contributor", contributorId));
        contributor.setLinkedUser(userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId)));
        return mapper.toResponse(contributorRepository.save(contributor));
    }

    @Override
    @Transactional
    public ContributorResponse unlinkUser(UUID contributorId) {
        Contributor contributor = contributorRepository.findById(contributorId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Contributor", contributorId));
        contributor.setLinkedUser(null);
        return mapper.toResponse(contributorRepository.save(contributor));
    }
}
