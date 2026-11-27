package com.openqualityhub.service.impl;

import com.openqualityhub.dto.response.DashboardResponse;
import com.openqualityhub.entity.*;
import com.openqualityhub.repository.*;
import com.openqualityhub.repository.TestIdentityRepository;
import com.openqualityhub.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
    @Cacheable(value = "dashboard", key = "'global'")
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
        BigDecimal avgQualityScore = qualityScoreRepository.findAverageOverallScore();

        return DashboardResponse.SummaryStats.builder()
            .totalTeams(teamRepository.countByDeletedFalse())
            .totalProjects(projectRepository.countByDeletedFalse())
            .totalTestRuns(testRunRepository.countByDeletedFalse())
            .totalTestCases(testCaseRepository.countByDeletedFalse())
            .totalDefects(defectRepository.countByDeletedFalse())
            .openDefects(defectRepository.countByStatusNotInAndDeletedFalse(
                List.of(Defect.DefectStatus.CLOSED, Defect.DefectStatus.RESOLVED)))
            .averagePassRate(avgPassRate != null ? avgPassRate.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
            .averageQualityScore(avgQualityScore != null ? avgQualityScore.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
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
        List<Project> projects = projectRepository.findByDeletedFalse();
        if (projects.isEmpty()) {
            return List.of();
        }

        // Batch-load all metrics in 3 aggregated queries instead of 4N individual queries
        Map<UUID, BigDecimal> passRates = toMap(testRunRepository.findAveragePassRateGroupedByProject());
        Map<UUID, Long> runCounts = toLongMap(testRunRepository.countGroupedByProject());
        Map<UUID, Long> openDefects = toLongMap(defectRepository.countOpenGroupedByProject());

        // Quality scores: latest per project (small dataset, single query)
        Map<UUID, BigDecimal> qualityScores = new HashMap<>();
        for (Project project : projects) {
            qualityScoreRepository.findFirstByProjectIdOrderByCalculatedAtDesc(project.getId())
                .ifPresent(qs -> qualityScores.put(project.getId(), qs.getOverallScore()));
        }

        return projects.stream().map(project -> {
            UUID pid = project.getId();
            BigDecimal passRate = passRates.get(pid);
            BigDecimal qualityScore = qualityScores.get(pid);

            return DashboardResponse.ProjectHealth.builder()
                .projectId(pid.toString())
                .projectName(project.getName())
                .teamName(project.getTeam() != null ? project.getTeam().getName() : null)
                .passRate(passRate != null ? passRate.setScale(2, RoundingMode.HALF_UP) : null)
                .qualityScore(qualityScore != null ? qualityScore.setScale(2, RoundingMode.HALF_UP) : null)
                .openDefects(openDefects.getOrDefault(pid, 0L))
                .totalTestRuns(runCounts.getOrDefault(pid, 0L))
                .status(project.getStatus().name())
                .build();
        }).collect(Collectors.toList());
    }

    private List<DashboardResponse.DefectSummary> buildDefectsByStatus() {
        Map<Defect.DefectStatus, Long> grouped = new HashMap<>();
        for (Object[] row : defectRepository.countGroupedByStatus()) {
            grouped.put((Defect.DefectStatus) row[0], (Long) row[1]);
        }
        return Arrays.stream(Defect.DefectStatus.values()).map(status ->
            DashboardResponse.DefectSummary.builder()
                .label(status.name())
                .count(grouped.getOrDefault(status, 0L))
                .build()
        ).collect(Collectors.toList());
    }

    private List<DashboardResponse.DefectSummary> buildDefectsBySeverity() {
        Map<Defect.Severity, Long> grouped = new HashMap<>();
        for (Object[] row : defectRepository.countGroupedBySeverity()) {
            grouped.put((Defect.Severity) row[0], (Long) row[1]);
        }
        return Arrays.stream(Defect.Severity.values()).map(severity ->
            DashboardResponse.DefectSummary.builder()
                .label(severity.name())
                .count(grouped.getOrDefault(severity, 0L))
                .build()
        ).collect(Collectors.toList());
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

    private Map<UUID, BigDecimal> toMap(List<Object[]> rows) {
        Map<UUID, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null && row[1] != null) {
                map.put((UUID) row[0], (BigDecimal) row[1]);
            }
        }
        return map;
    }

    private Map<UUID, Long> toLongMap(List<Object[]> rows) {
        Map<UUID, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null) {
                map.put((UUID) row[0], ((Number) row[1]).longValue());
            }
        }
        return map;
    }
}
