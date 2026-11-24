package com.openqualityhub.service.impl;

import com.openqualityhub.dto.response.DashboardResponse;
import com.openqualityhub.entity.*;
import com.openqualityhub.repository.*;
import com.openqualityhub.repository.TestIdentityRepository;
import com.openqualityhub.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;
    private final TestRunRepository testRunRepository;
    private final TestCaseRepository testCaseRepository;
    private final DefectRepository defectRepository;
    private final QualityScoreRepository qualityScoreRepository;
    private final TestIdentityRepository testIdentityRepository;

    @Override
    public DashboardResponse getDashboard() {
        return DashboardResponse.builder()
            .summary(buildSummary())
            .passRateTrend(buildPassRateTrend())
            .projectHealth(buildProjectHealth())
            .defectsByStatus(buildDefectsByStatus())
            .defectsBySeverity(buildDefectsBySeverity())
            .recentTestRuns(buildRecentTestRuns())
            .build();
    }

    private DashboardResponse.SummaryStats buildSummary() {
        BigDecimal avgPassRate = testRunRepository.findAveragePassRate();
        BigDecimal avgQualityScore = qualityScoreRepository.findByDeletedFalse().stream()
            .map(QualityScore::getOverallScore)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long scoreCount = qualityScoreRepository.countByDeletedFalse();
        if (scoreCount > 0) {
            avgQualityScore = avgQualityScore.divide(BigDecimal.valueOf(scoreCount), 2, RoundingMode.HALF_UP);
        }

        return DashboardResponse.SummaryStats.builder()
            .totalTeams(teamRepository.countByDeletedFalse())
            .totalProjects(projectRepository.countByDeletedFalse())
            .totalTestRuns(testRunRepository.countByDeletedFalse())
            .totalTestCases(testCaseRepository.countByDeletedFalse())
            .totalDefects(defectRepository.countByDeletedFalse())
            .openDefects(defectRepository.countByStatusNotInAndDeletedFalse(
                List.of(Defect.DefectStatus.CLOSED, Defect.DefectStatus.RESOLVED)))
            .averagePassRate(avgPassRate != null ? avgPassRate.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
            .averageQualityScore(avgQualityScore)
            .flakyTestCount(testIdentityRepository.countAllFlaky())
            .build();
    }

    private List<DashboardResponse.PassRateTrend> buildPassRateTrend() {
        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
        List<Object[]> rows = testRunRepository.findDailyPassRateTrend(since);

        return rows.stream().map(row -> DashboardResponse.PassRateTrend.builder()
            .date(row[0].toString())
            .passRate(row[1] != null ? ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
            .totalTests(((Number) row[2]).longValue())
            .passed(((Number) row[3]).longValue())
            .failed(((Number) row[4]).longValue())
            .build()
        ).collect(Collectors.toList());
    }

    private List<DashboardResponse.ProjectHealth> buildProjectHealth() {
        return projectRepository.findByDeletedFalse().stream().map(project -> {
            BigDecimal passRate = testRunRepository.findAveragePassRateByProjectId(project.getId());
            BigDecimal qualityScore = qualityScoreRepository.findFirstByProjectIdOrderByCalculatedAtDesc(project.getId())
                .map(QualityScore::getOverallScore)
                .orElse(null);
            long openDefects = defectRepository.countOpenByProjectId(project.getId());
            long totalRuns = testRunRepository.countByProjectIdAndDeletedFalse(project.getId());

            return DashboardResponse.ProjectHealth.builder()
                .projectId(project.getId().toString())
                .projectName(project.getName())
                .teamName(project.getTeam() != null ? project.getTeam().getName() : null)
                .passRate(passRate != null ? passRate.setScale(2, RoundingMode.HALF_UP) : null)
                .qualityScore(qualityScore != null ? qualityScore.setScale(2, RoundingMode.HALF_UP) : null)
                .openDefects(openDefects)
                .totalTestRuns(totalRuns)
                .status(project.getStatus().name())
                .build();
        }).collect(Collectors.toList());
    }

    private List<DashboardResponse.DefectSummary> buildDefectsByStatus() {
        return Arrays.stream(Defect.DefectStatus.values()).map(status -> {
            long count = defectRepository.countByStatusAndDeletedFalse(status);
            return DashboardResponse.DefectSummary.builder()
                .label(status.name())
                .count(count)
                .build();
        }).collect(Collectors.toList());
    }

    private List<DashboardResponse.DefectSummary> buildDefectsBySeverity() {
        return Arrays.stream(Defect.Severity.values()).map(severity -> {
            long count = defectRepository.countBySeverityAndDeletedFalse(severity);
            return DashboardResponse.DefectSummary.builder()
                .label(severity.name())
                .count(count)
                .build();
        }).collect(Collectors.toList());
    }

    private List<DashboardResponse.TestRunSummary> buildRecentTestRuns() {
        return testRunRepository.findByDeletedFalseOrderByExecutedAtDesc(PageRequest.of(0, 10))
            .getContent().stream().map(tr -> DashboardResponse.TestRunSummary.builder()
                .id(tr.getId().toString())
                .name(tr.getName())
                .projectName(tr.getProject() != null ? tr.getProject().getName() : null)
                .source(tr.getSource().name())
                .totalTests(tr.getTotalTests())
                .passed(tr.getPassed())
                .failed(tr.getFailed())
                .skipped(tr.getSkipped())
                .passRate(tr.getPassRate())
                .executedAt(tr.getExecutedAt() != null ? tr.getExecutedAt().toString() : null)
                .build()
            ).collect(Collectors.toList());
    }
}
