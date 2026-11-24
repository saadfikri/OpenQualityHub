package com.openqualityhub.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openqualityhub.dto.request.CreateSignOffRequest;
import com.openqualityhub.dto.response.ReadinessReportResponse;
import com.openqualityhub.dto.response.ReleaseDashboardResponse;
import com.openqualityhub.dto.response.SignOffResponse;
import com.openqualityhub.entity.*;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.repository.*;
import com.openqualityhub.service.ReleaseConfidenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReleaseConfidenceServiceImpl implements ReleaseConfidenceService {

    private static final UUID DEFAULT_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final ReleaseRepository releaseRepository;
    private final ReleaseReadinessReportRepository reportRepository;
    private final ReleaseSignOffRepository signOffRepository;
    private final QualityGateRepository qualityGateRepository;
    private final TestRunRepository testRunRepository;
    private final DefectRepository defectRepository;
    private final BuildRepository buildRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ReadinessReportResponse generateReadinessReport(UUID releaseId, UUID generatedById) {
        Release release = releaseRepository.findByIdAndDeletedFalse(releaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Release", releaseId));

        QualityGate gate = qualityGateRepository.findByOrganizationIdAndDefaultGateTrue(DEFAULT_ORG_ID)
                .orElseThrow(() -> new ResourceNotFoundException("QualityGate", "default"));

        // Compute live metrics
        BigDecimal passRate = testRunRepository.findAveragePassRateByReleaseId(releaseId);
        if (passRate == null) passRate = BigDecimal.ZERO;

        long totalTestRuns = testRunRepository.countByReleaseIdAndDeletedFalse(releaseId);
        long openDefects = defectRepository.countOpenByReleaseId(releaseId);
        long criticalDefects = defectRepository.countByReleaseIdAndSeverityAndStatusNotAndDeletedFalse(
                releaseId, Defect.Severity.CRITICAL, Defect.DefectStatus.CLOSED);
        long highDefects = defectRepository.countByReleaseIdAndSeverityAndStatusNotAndDeletedFalse(
                releaseId, Defect.Severity.HIGH, Defect.DefectStatus.CLOSED);
        long totalBuilds = buildRepository.countByReleaseIdAndDeletedFalse(releaseId);
        long successfulBuilds = buildRepository.countByReleaseIdAndStatusAndDeletedFalse(releaseId, Build.BuildStatus.SUCCESS);
        BigDecimal buildSuccessRate = totalBuilds > 0
                ? BigDecimal.valueOf(successfulBuilds * 100.0 / totalBuilds).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, BigDecimal> metricValues = Map.of(
                "pass_rate", passRate,
                "critical_defects", BigDecimal.valueOf(criticalDefects),
                "high_defects", BigDecimal.valueOf(highDefects),
                "open_defects", BigDecimal.valueOf(openDefects),
                "build_success_rate", buildSuccessRate,
                "total_test_runs", BigDecimal.valueOf(totalTestRuns)
        );

        // Evaluate each gate condition
        List<ReleaseGateResult> gateResults = new ArrayList<>();
        boolean hasCriticalFail = false;
        boolean hasNonCriticalFail = false;

        for (QualityGateCondition condition : gate.getConditions()) {
            BigDecimal actual = metricValues.getOrDefault(condition.getMetricKey(), BigDecimal.ZERO);
            BigDecimal expected = condition.getThresholdValue();
            boolean passes = evaluateCondition(actual, expected, condition.getOperator());

            ReleaseGateResult.ResultStatus resultStatus = passes
                    ? ReleaseGateResult.ResultStatus.PASS
                    : ReleaseGateResult.ResultStatus.FAIL;

            if (!passes) {
                if (condition.isCritical()) hasCriticalFail = true;
                else hasNonCriticalFail = true;
            }

            gateResults.add(ReleaseGateResult.builder()
                    .metricKey(condition.getMetricKey())
                    .condition(condition)
                    .actualValue(actual)
                    .expectedValue(expected)
                    .operator(condition.getOperator())
                    .status(resultStatus)
                    .critical(condition.isCritical())
                    .build());
        }

        ReleaseReadinessReport.GateStatus gateStatus;
        if (hasCriticalFail) gateStatus = ReleaseReadinessReport.GateStatus.FAIL;
        else if (hasNonCriticalFail) gateStatus = ReleaseReadinessReport.GateStatus.WARN;
        else gateStatus = ReleaseReadinessReport.GateStatus.PASS;

        // Compute overall score (weighted: pass_rate 40%, build health 30%, defect health 30%)
        BigDecimal defectScore = BigDecimal.valueOf(100);
        if (criticalDefects > 0) defectScore = BigDecimal.ZERO;
        else if (highDefects > 0) defectScore = BigDecimal.valueOf(Math.max(0, 100 - highDefects * 20));
        else if (openDefects > 0) defectScore = BigDecimal.valueOf(Math.max(50, 100 - openDefects * 5));

        BigDecimal overallScore = passRate.multiply(new BigDecimal("0.40"))
                .add(buildSuccessRate.multiply(new BigDecimal("0.30")))
                .add(defectScore.multiply(new BigDecimal("0.30")))
                .setScale(2, RoundingMode.HALF_UP);

        // Build metrics JSON
        ReadinessReportResponse.MetricsSnapshot metricsSnapshot = ReadinessReportResponse.MetricsSnapshot.builder()
                .passRate(passRate)
                .totalTestRuns(totalTestRuns)
                .openDefects(openDefects)
                .criticalDefects(criticalDefects)
                .highDefects(highDefects)
                .totalBuilds(totalBuilds)
                .successfulBuilds(successfulBuilds)
                .buildSuccessRate(buildSuccessRate)
                .build();

        String metricsJson;
        try {
            metricsJson = objectMapper.writeValueAsString(metricsSnapshot);
        } catch (Exception e) {
            metricsJson = "{}";
        }

        // Build summary
        String summary = String.format(
                "Release %s %s: overall score %.1f%%, gate %s. Pass rate: %.1f%%, Open defects: %d (critical: %d), Build success: %.1f%%",
                release.getReleaseVersion(), release.getName() != null ? release.getName() : "",
                overallScore, gateStatus, passRate, openDefects, criticalDefects, buildSuccessRate
        );

        // Persist report
        User generatedBy = generatedById != null ? userRepository.findById(generatedById).orElse(null) : null;

        ReleaseReadinessReport report = ReleaseReadinessReport.builder()
                .release(release)
                .overallScore(overallScore)
                .gateStatus(gateStatus)
                .summary(summary)
                .metrics(metricsJson)
                .generatedAt(Instant.now())
                .generatedBy(generatedBy)
                .build();

        for (ReleaseGateResult result : gateResults) {
            result.setReport(report);
            report.getGateResults().add(result);
        }

        report = reportRepository.save(report);
        log.info("Generated readiness report for release {} -> {} (score: {})", releaseId, gateStatus, overallScore);
        return toReportResponse(report, metricsSnapshot);
    }

    @Override
    public ReadinessReportResponse getLatestReadinessReport(UUID releaseId) {
        ReleaseReadinessReport report = reportRepository.findFirstByReleaseIdAndDeletedFalseOrderByGeneratedAtDesc(releaseId)
                .orElseThrow(() -> new ResourceNotFoundException("ReadinessReport", releaseId));
        return toReportResponse(report, parseMetrics(report.getMetrics()));
    }

    @Override
    public Page<ReadinessReportResponse> getReadinessReportHistory(UUID releaseId, Pageable pageable) {
        return reportRepository.findByReleaseIdAndDeletedFalseOrderByGeneratedAtDesc(releaseId, pageable)
                .map(r -> toReportResponse(r, parseMetrics(r.getMetrics())));
    }

    @Override
    @Transactional
    public SignOffResponse signOff(UUID releaseId, CreateSignOffRequest request, UUID userId) {
        Release release = releaseRepository.findByIdAndDeletedFalse(releaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Release", releaseId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        String roleName = user.getRoles().stream()
                .map(Role::getName)
                .findFirst()
                .orElse("UNKNOWN");

        ReleaseSignOff.Decision decision = ReleaseSignOff.Decision.valueOf(request.getDecision());

        // Check if user already signed off (allow update)
        Optional<ReleaseSignOff> existing = signOffRepository.findByReleaseIdAndUserIdAndDeletedFalse(releaseId, userId);
        ReleaseSignOff signOff;
        if (existing.isPresent()) {
            signOff = existing.get();
            signOff.setDecision(decision);
            signOff.setComment(request.getComment());
            signOff.setDecidedAt(Instant.now());
        } else {
            signOff = ReleaseSignOff.builder()
                    .release(release)
                    .user(user)
                    .role(roleName)
                    .decision(decision)
                    .comment(request.getComment())
                    .decidedAt(Instant.now())
                    .build();
        }

        signOff = signOffRepository.save(signOff);
        log.info("Release {} sign-off by {} ({}): {}", releaseId, user.getDisplayName(), roleName, decision);
        return toSignOffResponse(signOff);
    }

    @Override
    public List<SignOffResponse> getSignOffs(UUID releaseId) {
        return signOffRepository.findByReleaseIdAndDeletedFalse(releaseId).stream()
                .map(this::toSignOffResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ReleaseDashboardResponse getReleaseDashboard() {
        List<Release.ReleaseStatus> activeStatuses = List.of(
                Release.ReleaseStatus.PLANNING,
                Release.ReleaseStatus.IN_PROGRESS,
                Release.ReleaseStatus.READY_FOR_QA,
                Release.ReleaseStatus.QA_IN_PROGRESS,
                Release.ReleaseStatus.APPROVED
        );

        List<Release> activeReleases = releaseRepository.findByStatusInAndDeletedFalse(activeStatuses);

        List<ReleaseDashboardResponse.ReleaseReadinessSummary> summaries = new ArrayList<>();
        long passing = 0, warning = 0, failing = 0;

        for (Release release : activeReleases) {
            Optional<ReleaseReadinessReport> latestReport =
                    reportRepository.findFirstByReleaseIdAndDeletedFalseOrderByGeneratedAtDesc(release.getId());

            long approved = signOffRepository.countByReleaseIdAndDecisionAndDeletedFalse(
                    release.getId(), ReleaseSignOff.Decision.APPROVED);
            long rejected = signOffRepository.countByReleaseIdAndDecisionAndDeletedFalse(
                    release.getId(), ReleaseSignOff.Decision.REJECTED);

            String gateStatus = null;
            BigDecimal score = null;
            String lastEvaluated = null;

            if (latestReport.isPresent()) {
                ReleaseReadinessReport rpt = latestReport.get();
                gateStatus = rpt.getGateStatus().name();
                score = rpt.getOverallScore();
                lastEvaluated = rpt.getGeneratedAt().toString();

                switch (rpt.getGateStatus()) {
                    case PASS -> passing++;
                    case WARN -> warning++;
                    case FAIL -> failing++;
                }
            }

            summaries.add(ReleaseDashboardResponse.ReleaseReadinessSummary.builder()
                    .releaseId(release.getId().toString())
                    .releaseName(release.getName())
                    .releaseVersion(release.getReleaseVersion())
                    .projectName(release.getProject().getName())
                    .releaseStatus(release.getStatus().name())
                    .targetDate(release.getTargetDate() != null ? release.getTargetDate().toString() : null)
                    .overallScore(score)
                    .gateStatus(gateStatus)
                    .approvedCount(approved)
                    .rejectedCount(rejected)
                    .lastEvaluatedAt(lastEvaluated)
                    .build());
        }

        return ReleaseDashboardResponse.builder()
                .activeReleases(summaries)
                .totalActiveReleases(activeReleases.size())
                .passingReleases(passing)
                .warningReleases(warning)
                .failingReleases(failing)
                .build();
    }

    // --- Helpers ---

    private boolean evaluateCondition(BigDecimal actual, BigDecimal expected, QualityGateCondition.Operator operator) {
        int cmp = actual.compareTo(expected);
        return switch (operator) {
            case GTE -> cmp >= 0;
            case GT -> cmp > 0;
            case LTE -> cmp <= 0;
            case LT -> cmp < 0;
            case EQ -> cmp == 0;
        };
    }

    private ReadinessReportResponse toReportResponse(ReleaseReadinessReport report,
                                                      ReadinessReportResponse.MetricsSnapshot metrics) {
        List<ReadinessReportResponse.GateResultItem> items = report.getGateResults().stream()
                .map(r -> ReadinessReportResponse.GateResultItem.builder()
                        .id(r.getId() != null ? r.getId().toString() : null)
                        .conditionId(r.getCondition() != null ? r.getCondition().getId().toString() : null)
                        .metricKey(r.getMetricKey())
                        .actualValue(r.getActualValue())
                        .expectedValue(r.getExpectedValue())
                        .operator(r.getOperator().name())
                        .status(r.getStatus().name())
                        .critical(r.isCritical())
                        .build())
                .collect(Collectors.toList());

        return ReadinessReportResponse.builder()
                .id(report.getId() != null ? report.getId().toString() : null)
                .releaseId(report.getRelease().getId().toString())
                .releaseName(report.getRelease().getName())
                .releaseVersion(report.getRelease().getReleaseVersion())
                .overallScore(report.getOverallScore())
                .gateStatus(report.getGateStatus().name())
                .summary(report.getSummary())
                .generatedAt(report.getGeneratedAt() != null ? report.getGeneratedAt().toString() : null)
                .generatedByName(report.getGeneratedBy() != null ? report.getGeneratedBy().getDisplayName() : null)
                .gateResults(items)
                .metrics(metrics)
                .build();
    }

    private SignOffResponse toSignOffResponse(ReleaseSignOff signOff) {
        return SignOffResponse.builder()
                .id(signOff.getId() != null ? signOff.getId().toString() : null)
                .releaseId(signOff.getRelease().getId().toString())
                .userId(signOff.getUser().getId().toString())
                .userName(signOff.getUser().getDisplayName())
                .role(signOff.getRole())
                .decision(signOff.getDecision().name())
                .comment(signOff.getComment())
                .decidedAt(signOff.getDecidedAt() != null ? signOff.getDecidedAt().toString() : null)
                .build();
    }

    private ReadinessReportResponse.MetricsSnapshot parseMetrics(String metricsJson) {
        try {
            return objectMapper.readValue(metricsJson, ReadinessReportResponse.MetricsSnapshot.class);
        } catch (Exception e) {
            return ReadinessReportResponse.MetricsSnapshot.builder().build();
        }
    }
}
