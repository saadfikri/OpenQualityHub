package com.openqualityhub.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openqualityhub.dto.request.CreateSignOffRequest;
import com.openqualityhub.dto.response.ReadinessReportResponse;
import com.openqualityhub.dto.response.ReleaseDashboardResponse;
import com.openqualityhub.dto.response.SignOffResponse;
import com.openqualityhub.entity.*;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReleaseConfidenceServiceImplTest {

    private static final UUID DEFAULT_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock private ReleaseRepository releaseRepository;
    @Mock private ReleaseReadinessReportRepository reportRepository;
    @Mock private ReleaseSignOffRepository signOffRepository;
    @Mock private QualityGateRepository qualityGateRepository;
    @Mock private TestRunRepository testRunRepository;
    @Mock private DefectRepository defectRepository;
    @Mock private BuildRepository buildRepository;
    @Mock private UserRepository userRepository;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private ReleaseConfidenceServiceImpl releaseConfidenceService;

    private UUID releaseId;
    private UUID userId;
    private Release release;
    private Project project;
    private User user;
    private QualityGate qualityGate;

    @BeforeEach
    void setUp() {
        releaseId = UUID.randomUUID();
        userId = UUID.randomUUID();

        project = Project.builder().name("Test Project").build();
        project.setId(UUID.randomUUID());

        release = Release.builder()
            .releaseVersion("1.0.0")
            .name("Test Release")
            .project(project)
            .status(Release.ReleaseStatus.QA_IN_PROGRESS)
            .build();
        release.setId(releaseId);

        Role role = Role.builder().name("QA_MANAGER").build();
        role.setId(UUID.randomUUID());

        user = User.builder()
            .displayName("Sarah Chen")
            .email("sarah@test.com")
            .firstName("Sarah")
            .lastName("Chen")
            .passwordHash("hash")
            .roles(new HashSet<>(Set.of(role)))
            .build();
        user.setId(userId);

        qualityGate = QualityGate.builder()
            .name("Default Gate")
            .defaultGate(true)
            .conditions(new ArrayList<>())
            .build();
        qualityGate.setId(UUID.randomUUID());
    }

    @Test
    void generateReport_shouldCreateReport() throws Exception {
        when(releaseRepository.findByIdAndDeletedFalse(releaseId)).thenReturn(Optional.of(release));
        when(qualityGateRepository.findByOrganizationIdAndDefaultGateTrue(DEFAULT_ORG_ID))
            .thenReturn(Optional.of(qualityGate));
        when(testRunRepository.findAveragePassRateByReleaseId(releaseId)).thenReturn(new BigDecimal("95.00"));
        when(testRunRepository.countByReleaseIdAndDeletedFalse(releaseId)).thenReturn(10L);
        when(defectRepository.countOpenByReleaseId(releaseId)).thenReturn(2L);
        when(defectRepository.countByReleaseIdAndSeverityAndStatusNotAndDeletedFalse(
            eq(releaseId), eq(Defect.Severity.CRITICAL), eq(Defect.DefectStatus.CLOSED))).thenReturn(0L);
        when(defectRepository.countByReleaseIdAndSeverityAndStatusNotAndDeletedFalse(
            eq(releaseId), eq(Defect.Severity.HIGH), eq(Defect.DefectStatus.CLOSED))).thenReturn(1L);
        when(buildRepository.countByReleaseIdAndDeletedFalse(releaseId)).thenReturn(5L);
        when(buildRepository.countByReleaseIdAndStatusAndDeletedFalse(releaseId, Build.BuildStatus.SUCCESS)).thenReturn(4L);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"passRate\":95.00}");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(reportRepository.save(any(ReleaseReadinessReport.class))).thenAnswer(inv -> {
            ReleaseReadinessReport r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        ReadinessReportResponse result = releaseConfidenceService.generateReadinessReport(releaseId, userId);

        assertNotNull(result);
        assertNotNull(result.getOverallScore());
        verify(reportRepository).save(any(ReleaseReadinessReport.class));
    }

    @Test
    void generateReport_shouldThrowWhenReleaseNotFound() {
        when(releaseRepository.findByIdAndDeletedFalse(releaseId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> releaseConfidenceService.generateReadinessReport(releaseId, userId));
    }

    @Test
    void generateReport_shouldCalculateScore() throws Exception {
        // pass_rate=80, build_success=100 (2/2), no defects -> defect_score=100
        // score = 80*0.40 + 100*0.30 + 100*0.30 = 32 + 30 + 30 = 92.00
        when(releaseRepository.findByIdAndDeletedFalse(releaseId)).thenReturn(Optional.of(release));
        when(qualityGateRepository.findByOrganizationIdAndDefaultGateTrue(DEFAULT_ORG_ID))
            .thenReturn(Optional.of(qualityGate));
        when(testRunRepository.findAveragePassRateByReleaseId(releaseId)).thenReturn(new BigDecimal("80.00"));
        when(testRunRepository.countByReleaseIdAndDeletedFalse(releaseId)).thenReturn(5L);
        when(defectRepository.countOpenByReleaseId(releaseId)).thenReturn(0L);
        when(defectRepository.countByReleaseIdAndSeverityAndStatusNotAndDeletedFalse(
            eq(releaseId), eq(Defect.Severity.CRITICAL), eq(Defect.DefectStatus.CLOSED))).thenReturn(0L);
        when(defectRepository.countByReleaseIdAndSeverityAndStatusNotAndDeletedFalse(
            eq(releaseId), eq(Defect.Severity.HIGH), eq(Defect.DefectStatus.CLOSED))).thenReturn(0L);
        when(buildRepository.countByReleaseIdAndDeletedFalse(releaseId)).thenReturn(2L);
        when(buildRepository.countByReleaseIdAndStatusAndDeletedFalse(releaseId, Build.BuildStatus.SUCCESS)).thenReturn(2L);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(reportRepository.save(any(ReleaseReadinessReport.class))).thenAnswer(inv -> {
            ReleaseReadinessReport r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        ReadinessReportResponse result = releaseConfidenceService.generateReadinessReport(releaseId, null);

        assertNotNull(result);
        assertEquals(new BigDecimal("92.00"), result.getOverallScore());
    }

    @Test
    void generateReport_shouldSetGateStatusToFailOnCriticalDefects() throws Exception {
        // Create a critical condition: pass_rate >= 90, actual is 50 -> FAIL
        QualityGateCondition condition = QualityGateCondition.builder()
            .metricKey("pass_rate")
            .operator(QualityGateCondition.Operator.GTE)
            .thresholdValue(new BigDecimal("90.00"))
            .critical(true)
            .build();
        condition.setId(UUID.randomUUID());
        qualityGate.setConditions(List.of(condition));

        when(releaseRepository.findByIdAndDeletedFalse(releaseId)).thenReturn(Optional.of(release));
        when(qualityGateRepository.findByOrganizationIdAndDefaultGateTrue(DEFAULT_ORG_ID))
            .thenReturn(Optional.of(qualityGate));
        when(testRunRepository.findAveragePassRateByReleaseId(releaseId)).thenReturn(new BigDecimal("50.00"));
        when(testRunRepository.countByReleaseIdAndDeletedFalse(releaseId)).thenReturn(1L);
        when(defectRepository.countOpenByReleaseId(releaseId)).thenReturn(0L);
        when(defectRepository.countByReleaseIdAndSeverityAndStatusNotAndDeletedFalse(
            eq(releaseId), eq(Defect.Severity.CRITICAL), eq(Defect.DefectStatus.CLOSED))).thenReturn(0L);
        when(defectRepository.countByReleaseIdAndSeverityAndStatusNotAndDeletedFalse(
            eq(releaseId), eq(Defect.Severity.HIGH), eq(Defect.DefectStatus.CLOSED))).thenReturn(0L);
        when(buildRepository.countByReleaseIdAndDeletedFalse(releaseId)).thenReturn(0L);
        when(buildRepository.countByReleaseIdAndStatusAndDeletedFalse(releaseId, Build.BuildStatus.SUCCESS)).thenReturn(0L);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(reportRepository.save(any(ReleaseReadinessReport.class))).thenAnswer(inv -> {
            ReleaseReadinessReport r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        ReadinessReportResponse result = releaseConfidenceService.generateReadinessReport(releaseId, null);

        assertEquals("FAIL", result.getGateStatus());
        assertEquals(1, result.getGateResults().size());
        assertEquals("FAIL", result.getGateResults().get(0).getStatus());
    }

    @Test
    void getReportsByRelease_shouldReturnReports() {
        Pageable pageable = PageRequest.of(0, 10);
        ReleaseReadinessReport report = ReleaseReadinessReport.builder()
            .release(release)
            .overallScore(new BigDecimal("85.00"))
            .gateStatus(ReleaseReadinessReport.GateStatus.PASS)
            .summary("All good")
            .metrics("{}")
            .generatedAt(Instant.now())
            .gateResults(new ArrayList<>())
            .build();
        report.setId(UUID.randomUUID());

        Page<ReleaseReadinessReport> reportPage = new PageImpl<>(List.of(report), pageable, 1);

        when(reportRepository.findByReleaseIdAndDeletedFalseOrderByGeneratedAtDesc(releaseId, pageable))
            .thenReturn(reportPage);

        // parseMetrics uses objectMapper.readValue, need to mock that
        try {
            when(objectMapper.readValue(eq("{}"), eq(ReadinessReportResponse.MetricsSnapshot.class)))
                .thenReturn(ReadinessReportResponse.MetricsSnapshot.builder().build());
        } catch (Exception ignored) {}

        Page<ReadinessReportResponse> result = releaseConfidenceService.getReadinessReportHistory(releaseId, pageable);

        assertEquals(1, result.getTotalElements());
        verify(reportRepository).findByReleaseIdAndDeletedFalseOrderByGeneratedAtDesc(releaseId, pageable);
    }

    @Test
    void submitSignOff_shouldCreateSignOff() {
        CreateSignOffRequest request = CreateSignOffRequest.builder()
            .decision("APPROVED")
            .comment("LGTM")
            .build();

        when(releaseRepository.findByIdAndDeletedFalse(releaseId)).thenReturn(Optional.of(release));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(signOffRepository.findByReleaseIdAndUserIdAndDeletedFalse(releaseId, userId))
            .thenReturn(Optional.empty());
        when(signOffRepository.save(any(ReleaseSignOff.class))).thenAnswer(inv -> {
            ReleaseSignOff s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SignOffResponse result = releaseConfidenceService.signOff(releaseId, request, userId);

        assertNotNull(result);
        assertEquals("APPROVED", result.getDecision());
        assertEquals("LGTM", result.getComment());
        verify(signOffRepository).save(any(ReleaseSignOff.class));
    }

    @Test
    void submitSignOff_shouldThrowWhenReleaseNotFound() {
        CreateSignOffRequest request = CreateSignOffRequest.builder()
            .decision("APPROVED")
            .build();

        when(releaseRepository.findByIdAndDeletedFalse(releaseId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> releaseConfidenceService.signOff(releaseId, request, userId));
    }

    @Test
    void submitSignOff_shouldUpdateExistingSignOff() {
        CreateSignOffRequest request = CreateSignOffRequest.builder()
            .decision("REJECTED")
            .comment("Needs more testing")
            .build();

        ReleaseSignOff existingSignOff = ReleaseSignOff.builder()
            .release(release)
            .user(user)
            .role("QA_MANAGER")
            .decision(ReleaseSignOff.Decision.APPROVED)
            .comment("Old comment")
            .decidedAt(Instant.now().minusSeconds(3600))
            .build();
        existingSignOff.setId(UUID.randomUUID());

        when(releaseRepository.findByIdAndDeletedFalse(releaseId)).thenReturn(Optional.of(release));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(signOffRepository.findByReleaseIdAndUserIdAndDeletedFalse(releaseId, userId))
            .thenReturn(Optional.of(existingSignOff));
        when(signOffRepository.save(any(ReleaseSignOff.class))).thenReturn(existingSignOff);

        SignOffResponse result = releaseConfidenceService.signOff(releaseId, request, userId);

        assertNotNull(result);
        assertEquals("REJECTED", result.getDecision());
        assertEquals("Needs more testing", result.getComment());
    }

    @Test
    void getSignOffs_shouldReturnSignOffs() {
        ReleaseSignOff signOff = ReleaseSignOff.builder()
            .release(release)
            .user(user)
            .role("QA_MANAGER")
            .decision(ReleaseSignOff.Decision.APPROVED)
            .comment("Approved")
            .decidedAt(Instant.now())
            .build();
        signOff.setId(UUID.randomUUID());

        when(signOffRepository.findByReleaseIdAndDeletedFalse(releaseId)).thenReturn(List.of(signOff));

        List<SignOffResponse> result = releaseConfidenceService.getSignOffs(releaseId);

        assertEquals(1, result.size());
        assertEquals("APPROVED", result.get(0).getDecision());
        assertEquals("QA_MANAGER", result.get(0).getRole());
        verify(signOffRepository).findByReleaseIdAndDeletedFalse(releaseId);
    }

    @Test
    void getDashboard_shouldReturnStats() {
        when(releaseRepository.findByStatusInAndDeletedFalse(any())).thenReturn(List.of(release));
        ReleaseReadinessReport report = ReleaseReadinessReport.builder()
            .release(release)
            .overallScore(new BigDecimal("90.00"))
            .gateStatus(ReleaseReadinessReport.GateStatus.PASS)
            .generatedAt(Instant.now())
            .gateResults(new ArrayList<>())
            .build();
        report.setId(UUID.randomUUID());

        when(reportRepository.findFirstByReleaseIdAndDeletedFalseOrderByGeneratedAtDesc(releaseId))
            .thenReturn(Optional.of(report));
        when(signOffRepository.countByReleaseIdAndDecisionAndDeletedFalse(releaseId, ReleaseSignOff.Decision.APPROVED))
            .thenReturn(2L);
        when(signOffRepository.countByReleaseIdAndDecisionAndDeletedFalse(releaseId, ReleaseSignOff.Decision.REJECTED))
            .thenReturn(0L);

        ReleaseDashboardResponse result = releaseConfidenceService.getReleaseDashboard();

        assertNotNull(result);
        assertEquals(1, result.getTotalActiveReleases());
        assertEquals(1, result.getPassingReleases());
        assertEquals(0, result.getWarningReleases());
        assertEquals(0, result.getFailingReleases());
        assertEquals(1, result.getActiveReleases().size());
        assertEquals("PASS", result.getActiveReleases().get(0).getGateStatus());
    }

    @Test
    void getLatestReport_shouldReturnMostRecent() {
        ReleaseReadinessReport report = ReleaseReadinessReport.builder()
            .release(release)
            .overallScore(new BigDecimal("88.50"))
            .gateStatus(ReleaseReadinessReport.GateStatus.WARN)
            .summary("Some warnings")
            .metrics("{}")
            .generatedAt(Instant.now())
            .gateResults(new ArrayList<>())
            .build();
        report.setId(UUID.randomUUID());

        when(reportRepository.findFirstByReleaseIdAndDeletedFalseOrderByGeneratedAtDesc(releaseId))
            .thenReturn(Optional.of(report));

        try {
            when(objectMapper.readValue(eq("{}"), eq(ReadinessReportResponse.MetricsSnapshot.class)))
                .thenReturn(ReadinessReportResponse.MetricsSnapshot.builder().build());
        } catch (Exception ignored) {}

        ReadinessReportResponse result = releaseConfidenceService.getLatestReadinessReport(releaseId);

        assertNotNull(result);
        assertEquals(new BigDecimal("88.50"), result.getOverallScore());
        assertEquals("WARN", result.getGateStatus());
        verify(reportRepository).findFirstByReleaseIdAndDeletedFalseOrderByGeneratedAtDesc(releaseId);
    }

    @Test
    void getLatestReport_shouldThrowWhenNotFound() {
        when(reportRepository.findFirstByReleaseIdAndDeletedFalseOrderByGeneratedAtDesc(releaseId))
            .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> releaseConfidenceService.getLatestReadinessReport(releaseId));
    }
}
