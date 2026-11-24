package com.openqualityhub.service.impl;

import com.openqualityhub.dto.response.DashboardResponse;
import com.openqualityhub.entity.*;
import com.openqualityhub.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock private TeamRepository teamRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private TestRunRepository testRunRepository;
    @Mock private TestCaseRepository testCaseRepository;
    @Mock private DefectRepository defectRepository;
    @Mock private QualityScoreRepository qualityScoreRepository;
    @Mock private TestIdentityRepository testIdentityRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private Project project;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();

        Team team = Team.builder().name("Platform Team").build();
        team.setId(UUID.randomUUID());

        project = Project.builder()
            .name("Test Project")
            .status(Project.ProjectStatus.ACTIVE)
            .team(team)
            .build();
        project.setId(projectId);
    }

    @Test
    void getDashboard_shouldReturnSummaryStats() {
        // Summary stats
        when(teamRepository.countByDeletedFalse()).thenReturn(4L);
        when(projectRepository.countByDeletedFalse()).thenReturn(8L);
        when(testRunRepository.countByDeletedFalse()).thenReturn(240L);
        when(testCaseRepository.countByDeletedFalse()).thenReturn(1200L);
        when(defectRepository.countByDeletedFalse()).thenReturn(44L);
        when(defectRepository.countByStatusNotInAndDeletedFalse(any())).thenReturn(20L);
        when(testRunRepository.findAveragePassRate()).thenReturn(new BigDecimal("87.50"));
        when(qualityScoreRepository.findByDeletedFalse()).thenReturn(List.of());
        when(qualityScoreRepository.countByDeletedFalse()).thenReturn(0L);
        when(testIdentityRepository.countAllFlaky()).thenReturn(5L);

        // Pass rate trend
        when(testRunRepository.findDailyPassRateTrend(any(Instant.class))).thenReturn(List.of());

        // Project health
        when(projectRepository.findByDeletedFalse()).thenReturn(List.of(project));
        when(testRunRepository.findAveragePassRateByProjectId(projectId)).thenReturn(new BigDecimal("90.00"));
        when(qualityScoreRepository.findFirstByProjectIdOrderByCalculatedAtDesc(projectId)).thenReturn(Optional.empty());
        when(defectRepository.countOpenByProjectId(projectId)).thenReturn(3L);
        when(testRunRepository.countByProjectIdAndDeletedFalse(projectId)).thenReturn(50L);

        // Defects by status & severity
        for (Defect.DefectStatus status : Defect.DefectStatus.values()) {
            when(defectRepository.countByStatusAndDeletedFalse(status)).thenReturn(5L);
        }
        for (Defect.Severity severity : Defect.Severity.values()) {
            when(defectRepository.countBySeverityAndDeletedFalse(severity)).thenReturn(10L);
        }

        // Recent test runs
        when(testRunRepository.findByDeletedFalseOrderByExecutedAtDesc(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        DashboardResponse result = dashboardService.getDashboard();

        assertNotNull(result);
        assertNotNull(result.getSummary());
        assertEquals(4, result.getSummary().getTotalTeams());
        assertEquals(8, result.getSummary().getTotalProjects());
        assertEquals(240, result.getSummary().getTotalTestRuns());
        assertEquals(1200, result.getSummary().getTotalTestCases());
        assertEquals(44, result.getSummary().getTotalDefects());
        assertEquals(20, result.getSummary().getOpenDefects());
        assertEquals(5, result.getSummary().getFlakyTestCount());
    }

    @Test
    void getDashboard_shouldReturnProjectHealth() {
        stubSummary();
        stubTrend();
        stubDefectCounts();
        stubRecentRuns();

        when(projectRepository.findByDeletedFalse()).thenReturn(List.of(project));
        when(testRunRepository.findAveragePassRateByProjectId(projectId)).thenReturn(new BigDecimal("92.50"));
        QualityScore qs = QualityScore.builder().overallScore(new BigDecimal("85.00")).build();
        when(qualityScoreRepository.findFirstByProjectIdOrderByCalculatedAtDesc(projectId))
            .thenReturn(Optional.of(qs));
        when(defectRepository.countOpenByProjectId(projectId)).thenReturn(5L);
        when(testRunRepository.countByProjectIdAndDeletedFalse(projectId)).thenReturn(30L);

        DashboardResponse result = dashboardService.getDashboard();

        assertNotNull(result.getProjectHealth());
        assertEquals(1, result.getProjectHealth().size());
        DashboardResponse.ProjectHealth ph = result.getProjectHealth().get(0);
        assertEquals("Test Project", ph.getProjectName());
        assertEquals("Platform Team", ph.getTeamName());
        assertEquals(new BigDecimal("92.50"), ph.getPassRate());
        assertEquals(new BigDecimal("85.00"), ph.getQualityScore());
        assertEquals(5, ph.getOpenDefects());
        assertEquals(30, ph.getTotalTestRuns());
    }

    @Test
    void getDashboard_shouldReturnDefectsByStatus() {
        stubSummary();
        stubTrend();
        stubProjectHealth();
        stubRecentRuns();

        when(defectRepository.countByStatusAndDeletedFalse(Defect.DefectStatus.OPEN)).thenReturn(10L);
        when(defectRepository.countByStatusAndDeletedFalse(Defect.DefectStatus.IN_PROGRESS)).thenReturn(5L);
        when(defectRepository.countByStatusAndDeletedFalse(Defect.DefectStatus.RESOLVED)).thenReturn(15L);
        when(defectRepository.countByStatusAndDeletedFalse(Defect.DefectStatus.CLOSED)).thenReturn(20L);
        when(defectRepository.countByStatusAndDeletedFalse(Defect.DefectStatus.REOPENED)).thenReturn(2L);
        for (Defect.Severity severity : Defect.Severity.values()) {
            when(defectRepository.countBySeverityAndDeletedFalse(severity)).thenReturn(0L);
        }

        DashboardResponse result = dashboardService.getDashboard();

        assertNotNull(result.getDefectsByStatus());
        assertEquals(5, result.getDefectsByStatus().size());
        DashboardResponse.DefectSummary openSummary = result.getDefectsByStatus().stream()
            .filter(ds -> ds.getLabel().equals("OPEN"))
            .findFirst().orElseThrow();
        assertEquals(10, openSummary.getCount());
    }

    @Test
    void getDashboard_shouldReturnDefectsBySeverity() {
        stubSummary();
        stubTrend();
        stubProjectHealth();
        stubRecentRuns();
        for (Defect.DefectStatus status : Defect.DefectStatus.values()) {
            when(defectRepository.countByStatusAndDeletedFalse(status)).thenReturn(0L);
        }

        when(defectRepository.countBySeverityAndDeletedFalse(Defect.Severity.CRITICAL)).thenReturn(3L);
        when(defectRepository.countBySeverityAndDeletedFalse(Defect.Severity.HIGH)).thenReturn(8L);
        when(defectRepository.countBySeverityAndDeletedFalse(Defect.Severity.MEDIUM)).thenReturn(15L);
        when(defectRepository.countBySeverityAndDeletedFalse(Defect.Severity.LOW)).thenReturn(20L);

        DashboardResponse result = dashboardService.getDashboard();

        assertNotNull(result.getDefectsBySeverity());
        assertEquals(4, result.getDefectsBySeverity().size());
        DashboardResponse.DefectSummary criticalSummary = result.getDefectsBySeverity().stream()
            .filter(ds -> ds.getLabel().equals("CRITICAL"))
            .findFirst().orElseThrow();
        assertEquals(3, criticalSummary.getCount());
    }

    @Test
    void getDashboard_shouldReturnRecentTestRuns() {
        stubSummary();
        stubTrend();
        stubProjectHealth();
        stubDefectCounts();

        TestRun testRun = TestRun.builder()
            .name("Nightly Suite")
            .source(TestRun.TestSource.JUNIT)
            .totalTests(100)
            .passed(95)
            .failed(3)
            .skipped(2)
            .executedAt(Instant.now())
            .project(project)
            .build();
        testRun.setId(UUID.randomUUID());

        when(testRunRepository.findByDeletedFalseOrderByExecutedAtDesc(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(testRun)));

        DashboardResponse result = dashboardService.getDashboard();

        assertNotNull(result.getRecentTestRuns());
        assertEquals(1, result.getRecentTestRuns().size());
        DashboardResponse.TestRunSummary trs = result.getRecentTestRuns().get(0);
        assertEquals("Nightly Suite", trs.getName());
        assertEquals("JUNIT", trs.getSource());
        assertEquals(100, trs.getTotalTests());
        assertEquals(95, trs.getPassed());
        assertEquals("Test Project", trs.getProjectName());
    }

    @Test
    void getDashboard_shouldHandleNullAveragePassRate() {
        when(teamRepository.countByDeletedFalse()).thenReturn(0L);
        when(projectRepository.countByDeletedFalse()).thenReturn(0L);
        when(testRunRepository.countByDeletedFalse()).thenReturn(0L);
        when(testCaseRepository.countByDeletedFalse()).thenReturn(0L);
        when(defectRepository.countByDeletedFalse()).thenReturn(0L);
        when(defectRepository.countByStatusNotInAndDeletedFalse(any())).thenReturn(0L);
        when(testRunRepository.findAveragePassRate()).thenReturn(null);
        when(qualityScoreRepository.findByDeletedFalse()).thenReturn(List.of());
        when(qualityScoreRepository.countByDeletedFalse()).thenReturn(0L);
        when(testIdentityRepository.countAllFlaky()).thenReturn(0L);
        when(testRunRepository.findDailyPassRateTrend(any(Instant.class))).thenReturn(List.of());
        when(projectRepository.findByDeletedFalse()).thenReturn(List.of());
        for (Defect.DefectStatus status : Defect.DefectStatus.values()) {
            when(defectRepository.countByStatusAndDeletedFalse(status)).thenReturn(0L);
        }
        for (Defect.Severity severity : Defect.Severity.values()) {
            when(defectRepository.countBySeverityAndDeletedFalse(severity)).thenReturn(0L);
        }
        when(testRunRepository.findByDeletedFalseOrderByExecutedAtDesc(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        DashboardResponse result = dashboardService.getDashboard();

        assertNotNull(result.getSummary());
        assertEquals(BigDecimal.ZERO, result.getSummary().getAveragePassRate());
    }

    // --- Helpers to stub commonly needed calls ---

    private void stubSummary() {
        when(teamRepository.countByDeletedFalse()).thenReturn(1L);
        when(projectRepository.countByDeletedFalse()).thenReturn(1L);
        when(testRunRepository.countByDeletedFalse()).thenReturn(10L);
        when(testCaseRepository.countByDeletedFalse()).thenReturn(50L);
        when(defectRepository.countByDeletedFalse()).thenReturn(10L);
        when(defectRepository.countByStatusNotInAndDeletedFalse(any())).thenReturn(5L);
        when(testRunRepository.findAveragePassRate()).thenReturn(new BigDecimal("85.00"));
        when(qualityScoreRepository.findByDeletedFalse()).thenReturn(List.of());
        when(qualityScoreRepository.countByDeletedFalse()).thenReturn(0L);
        when(testIdentityRepository.countAllFlaky()).thenReturn(0L);
    }

    private void stubTrend() {
        when(testRunRepository.findDailyPassRateTrend(any(Instant.class))).thenReturn(List.of());
    }

    private void stubProjectHealth() {
        when(projectRepository.findByDeletedFalse()).thenReturn(List.of(project));
        when(testRunRepository.findAveragePassRateByProjectId(projectId)).thenReturn(null);
        when(qualityScoreRepository.findFirstByProjectIdOrderByCalculatedAtDesc(projectId)).thenReturn(Optional.empty());
        when(defectRepository.countOpenByProjectId(projectId)).thenReturn(0L);
        when(testRunRepository.countByProjectIdAndDeletedFalse(projectId)).thenReturn(0L);
    }

    private void stubDefectCounts() {
        for (Defect.DefectStatus status : Defect.DefectStatus.values()) {
            when(defectRepository.countByStatusAndDeletedFalse(status)).thenReturn(0L);
        }
        for (Defect.Severity severity : Defect.Severity.values()) {
            when(defectRepository.countBySeverityAndDeletedFalse(severity)).thenReturn(0L);
        }
    }

    private void stubRecentRuns() {
        when(testRunRepository.findByDeletedFalseOrderByExecutedAtDesc(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));
    }
}
