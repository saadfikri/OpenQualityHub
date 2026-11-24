package com.openqualityhub.service.impl;

import com.openqualityhub.dto.response.FlakyTestResponse;
import com.openqualityhub.dto.response.FlakyTestSummaryResponse;
import com.openqualityhub.entity.Project;
import com.openqualityhub.entity.TestIdentity;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.repository.ProjectRepository;
import com.openqualityhub.repository.TestIdentityRepository;
import com.openqualityhub.service.FlakyTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlakyTestServiceImpl implements FlakyTestService {

    private final TestIdentityRepository testIdentityRepository;
    private final ProjectRepository projectRepository;

    @Override
    public FlakyTestSummaryResponse getSummary() {
        long total = testIdentityRepository.count();
        long flaky = testIdentityRepository.countAllFlaky();
        BigDecimal percentage = total > 0
            ? BigDecimal.valueOf(flaky * 100.0 / total).setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        var byClassification = Arrays.stream(TestIdentity.FailureClassification.values())
            .map(c -> FlakyTestSummaryResponse.ClassificationCount.builder()
                .classification(c.name())
                .count(testIdentityRepository.countByFailureClassification(c))
                .build())
            .collect(Collectors.toList());

        var byProject = projectRepository.findByDeletedFalse().stream()
            .map(p -> FlakyTestSummaryResponse.ProjectFlakyCount.builder()
                .projectId(p.getId().toString())
                .projectName(p.getName())
                .flakyCount(testIdentityRepository.countByProjectIdAndFlakyTrue(p.getId()))
                .totalIdentities(testIdentityRepository.countByProjectId(p.getId()))
                .build())
            .collect(Collectors.toList());

        return FlakyTestSummaryResponse.builder()
            .totalTestIdentities(total)
            .flakyTestCount(flaky)
            .flakyPercentage(percentage)
            .byClassification(byClassification)
            .byProject(byProject)
            .build();
    }

    @Override
    public Page<FlakyTestResponse> getFlakyTests(UUID projectId, Pageable pageable) {
        if (projectId != null) {
            return testIdentityRepository.findByProjectIdAndFlakyTrueOrderByFlakyScoreDesc(projectId, pageable)
                .map(this::toResponse);
        }
        return testIdentityRepository.findByFlakyTrueOrderByFlakyScoreDesc(pageable)
            .map(this::toResponse);
    }

    @Override
    public Page<FlakyTestResponse> getAllTestIdentities(UUID projectId, Pageable pageable) {
        if (projectId != null) {
            return testIdentityRepository.findByProjectIdOrderByFlakyScoreDesc(projectId, pageable)
                .map(this::toResponse);
        }
        return testIdentityRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public FlakyTestResponse getById(UUID id) {
        return toResponse(testIdentityRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TestIdentity", id)));
    }

    private FlakyTestResponse toResponse(TestIdentity ti) {
        return FlakyTestResponse.builder()
            .id(ti.getId())
            .projectId(ti.getProject().getId())
            .projectName(ti.getProject().getName())
            .fingerprint(ti.getFingerprint())
            .className(ti.getClassName())
            .testName(ti.getTestName())
            .suiteName(ti.getSuiteName())
            .totalExecutions(ti.getTotalExecutions())
            .totalPassed(ti.getTotalPassed())
            .totalFailed(ti.getTotalFailed())
            .totalSkipped(ti.getTotalSkipped())
            .flakyScore(ti.getFlakyScore())
            .flaky(ti.isFlaky())
            .failureClassification(ti.getFailureClassification().name())
            .lastStatus(ti.getLastStatus())
            .avgDurationMs(ti.getAvgDurationMs())
            .firstSeenAt(ti.getFirstSeenAt())
            .lastSeenAt(ti.getLastSeenAt())
            .build();
    }
}
