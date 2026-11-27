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
        when(teamRepository.countByDeletedFalse()).thenReturn(4L);
        when(projectRepository.countByDeletedFalse()).thenReturn(8L);
        when(testRunRepository.countByDeletedFalse()).thenReturn(240L);
        when(testCaseRepository.countByDeletedFalse()).thenReturn(1200L);
        when(defectRepository.countByDeletedFalse()).thenReturn(44L);
        when(defectRepository.countByStatusNotInAndDeletedFalse(any())).thenReturn(20L);
        when(testRunRepository.findAveragePassRate()).thenReturn(new BigDecimal("87.50"));
        when(qualityScoreRepository.findAverageOverallScore()).thenReturn(null);
        when(testIdentityRepository.countAllFlaky()).thenReturn(5L);

        stubTrend();
        stubProjectHealth();
        stubDefectCounts();
        stubRecentRuns();

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
        Object[] passRateRow = {projectId, new BigDecimal("92.50")};
        when(testRunRepository.findAveragePassRateGroupedByProject())
            .thenReturn(List.<Object[]>of(passRateRow));
        Object[] countRow = {projectId, 30L};
        when(testRunRepository.countGroupedByProject())
            .thenReturn(List.<Object[]>of(countRow));
        Object[] openRow = {projectId, 5L};
        when(defectRepository.countOpenGroupedByProject())
            .thenReturn(List.<Object[]>of(openRow));
        QualityScore qs = QualityScore.builder().overallScore(new BigDecimal("85.00")).build();
        when(qualityScoreRepository.findFirstByProjectIdOrderByCalculatedAtDesc(projectId))
            .thenReturn(Optional.of(qs));

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

        Object[] s1 = {Defect.DefectStatus.OPEN, 10L};
        Object[] s2 = {Defect.DefectStatus.IN_PROGRESS, 5L};
        Object[] s3 = {Defect.DefectStatus.RESOLVED, 15L};
        Object[] s4 = {Defect.DefectStatus.CLOSED, 20L};
        Object[] s5 = {Defect.DefectStatus.REOPENED, 2L};
        when(defectRepository.countGroupedByStatus()).thenReturn(List.of(s1, s2, s3, s4, s5));
        when(defectRepository.countGroupedBySeverity()).thenReturn(List.of());

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

        when(defectRepository.countGroupedByStatus()).thenReturn(List.of());
        Object[] sv1 = {Defect.Severity.CRITICAL, 3L};
        Object[] sv2 = {Defect.Severity.HIGH, 8L};
        Object[] sv3 = {Defect.Severity.MEDIUM, 15L};
        Object[] sv4 = {Defect.Severity.LOW, 20L};
        when(defectRepository.countGroupedBySeverity()).thenReturn(List.of(sv1, sv2, sv3, sv4));

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
        when(qualityScoreRepository.findAverageOverallScore()).thenReturn(null);
        when(testIdentityRepository.countAllFlaky()).thenReturn(0L);
        when(testRunRepository.findDailyPassRateTrend(any(Instant.class))).thenReturn(List.of());
        when(projectRepository.findByDeletedFalse()).thenReturn(List.of());
        when(defectRepository.countGroupedByStatus()).thenReturn(List.of());
        when(defectRepository.countGroupedBySeverity()).thenReturn(List.of());
        when(testRunRepository.findByDeletedFalseOrderByExecutedAtDesc(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        DashboardResponse result = dashboardService.getDashboard();

        assertNotNull(result.getSummary());
        assertEquals(BigDecimal.ZERO, result.getSummary().getAveragePassRate());
    }

    // --- Helpers ---

    private void stubSummary() {
        when(teamRepository.countByDeletedFalse()).thenReturn(1L);
        when(projectRepository.countByDeletedFalse()).thenReturn(1L);
        when(testRunRepository.countByDeletedFalse()).thenReturn(10L);
        when(testCaseRepository.countByDeletedFalse()).thenReturn(50L);
        when(defectRepository.countByDeletedFalse()).thenReturn(10L);
        when(defectRepository.countByStatusNotInAndDeletedFalse(any())).thenReturn(5L);
        when(testRunRepository.findAveragePassRate()).thenReturn(new BigDecimal("85.00"));
        when(qualityScoreRepository.findAverageOverallScore()).thenReturn(new BigDecimal("80.00"));
        when(testIdentityRepository.countAllFlaky()).thenReturn(0L);
    }

    private void stubTrend() {
        when(testRunRepository.findDailyPassRateTrend(any(Instant.class))).thenReturn(List.of());
    }

    private void stubProjectHealth() {
        when(projectRepository.findByDeletedFalse()).thenReturn(List.of(project));
        when(testRunRepository.findAveragePassRateGroupedByProject()).thenReturn(List.of());
        when(testRunRepository.countGroupedByProject()).thenReturn(List.of());
        when(defectRepository.countOpenGroupedByProject()).thenReturn(List.of());
        when(qualityScoreRepository.findFirstByProjectIdOrderByCalculatedAtDesc(projectId)).thenReturn(Optional.empty());
    }

    private void stubDefectCounts() {
        when(defectRepository.countGroupedByStatus()).thenReturn(List.of());
        when(defectRepository.countGroupedBySeverity()).thenReturn(List.of());
    }

    private void stubRecentRuns() {
        when(testRunRepository.findByDeletedFalseOrderByExecutedAtDesc(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));
    }
}
