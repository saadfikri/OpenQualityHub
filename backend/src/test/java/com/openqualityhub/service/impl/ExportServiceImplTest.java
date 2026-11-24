package com.openqualityhub.service.impl;

import com.openqualityhub.entity.Defect;
import com.openqualityhub.entity.Project;
import com.openqualityhub.entity.TestRun;
import com.openqualityhub.entity.User;
import com.openqualityhub.repository.DefectRepository;
import com.openqualityhub.repository.TestRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportServiceImplTest {

    @Mock private TestRunRepository testRunRepository;
    @Mock private DefectRepository defectRepository;

    @InjectMocks
    private ExportServiceImpl exportService;

    private UUID projectId;
    private Project project;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        project = Project.builder().name("TestProject").build();
        project.setId(projectId);
    }

    // --- exportTestRunsCsv ---

    @Test
    void exportTestRunsCsv_withProjectId_filtersbyProject() {
        when(testRunRepository.findByProjectIdAndDeletedFalse(projectId))
                .thenReturn(Collections.emptyList());

        byte[] result = exportService.exportTestRunsCsv(projectId);

        String csv = new String(result, StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("ID,Name,Source,Project,"));
        verify(testRunRepository).findByProjectIdAndDeletedFalse(projectId);
        verify(testRunRepository, never()).findByDeletedFalse();
    }

    @Test
    void exportTestRunsCsv_withoutProjectId_getsAll() {
        when(testRunRepository.findByDeletedFalse()).thenReturn(Collections.emptyList());

        byte[] result = exportService.exportTestRunsCsv(null);

        assertNotNull(result);
        verify(testRunRepository).findByDeletedFalse();
    }

    @Test
    void exportTestRunsCsv_withData_producesCorrectCsv() {
        TestRun run = TestRun.builder()
                .name("Run 1")
                .source(TestRun.TestSource.JUNIT)
                .totalTests(10)
                .passed(8)
                .failed(1)
                .skipped(1)
                .durationMs(5000L)
                .environment("staging")
                .commitSha("abc123")
                .branch("main")
                .build();
        run.setId(UUID.randomUUID());
        run.setCreatedAt(Instant.parse("2026-11-01T10:00:00Z"));
        run.setProject(project);

        when(testRunRepository.findByDeletedFalse()).thenReturn(List.of(run));

        String csv = new String(exportService.exportTestRunsCsv(null), StandardCharsets.UTF_8);
        String[] lines = csv.split("\n");

        assertEquals(2, lines.length);
        assertTrue(lines[0].startsWith("ID,Name,Source,Project,"));
        assertTrue(lines[1].contains("Run 1"));
        assertTrue(lines[1].contains("JUNIT"));
        assertTrue(lines[1].contains("TestProject"));
        assertTrue(lines[1].contains("staging"));
        assertTrue(lines[1].contains("abc123"));
        assertTrue(lines[1].contains("main"));
    }

    @Test
    void exportTestRunsCsv_nullFields_handledGracefully() {
        TestRun run = TestRun.builder()
                .name("Minimal")
                .source(TestRun.TestSource.PLAYWRIGHT)
                .totalTests(1)
                .build();
        run.setId(UUID.randomUUID());
        run.setProject(null);

        when(testRunRepository.findByDeletedFalse()).thenReturn(List.of(run));

        String csv = new String(exportService.exportTestRunsCsv(null), StandardCharsets.UTF_8);

        // Should not throw NPE
        assertFalse(csv.isEmpty());
        assertTrue(csv.contains("Minimal"));
    }

    @Test
    void exportTestRunsCsv_escapesCommasInName() {
        TestRun run = TestRun.builder()
                .name("Run with, comma")
                .source(TestRun.TestSource.CYPRESS)
                .totalTests(5)
                .build();
        run.setId(UUID.randomUUID());
        run.setProject(project);

        when(testRunRepository.findByDeletedFalse()).thenReturn(List.of(run));

        String csv = new String(exportService.exportTestRunsCsv(null), StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"Run with, comma\""));
    }

    @Test
    void exportTestRunsCsv_escapesQuotesInName() {
        TestRun run = TestRun.builder()
                .name("Run \"quoted\"")
                .source(TestRun.TestSource.TESTNG)
                .totalTests(3)
                .build();
        run.setId(UUID.randomUUID());
        run.setProject(project);

        when(testRunRepository.findByDeletedFalse()).thenReturn(List.of(run));

        String csv = new String(exportService.exportTestRunsCsv(null), StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"Run \"\"quoted\"\"\""));
    }

    @Test
    void exportTestRunsCsv_escapesNewlinesInEnvironment() {
        TestRun run = TestRun.builder()
                .name("Run")
                .source(TestRun.TestSource.JUNIT)
                .totalTests(1)
                .environment("line1\nline2")
                .build();
        run.setId(UUID.randomUUID());
        run.setProject(project);

        when(testRunRepository.findByDeletedFalse()).thenReturn(List.of(run));

        String csv = new String(exportService.exportTestRunsCsv(null), StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"line1\nline2\""));
    }

    // --- exportDefectsCsv ---

    @Test
    void exportDefectsCsv_bothProjectIdAndStatus() {
        when(defectRepository.findAllByProjectIdAndStatusAndDeletedFalse(eq(projectId), eq(Defect.DefectStatus.OPEN)))
                .thenReturn(Collections.emptyList());

        byte[] result = exportService.exportDefectsCsv(projectId, "open");

        assertNotNull(result);
        verify(defectRepository).findAllByProjectIdAndStatusAndDeletedFalse(projectId, Defect.DefectStatus.OPEN);
    }

    @Test
    void exportDefectsCsv_onlyProjectId() {
        when(defectRepository.findAllByProjectIdAndDeletedFalse(projectId))
                .thenReturn(Collections.emptyList());

        exportService.exportDefectsCsv(projectId, null);

        verify(defectRepository).findAllByProjectIdAndDeletedFalse(projectId);
    }

    @Test
    void exportDefectsCsv_onlyStatus() {
        when(defectRepository.findAllByStatusAndDeletedFalse(Defect.DefectStatus.RESOLVED))
                .thenReturn(Collections.emptyList());

        exportService.exportDefectsCsv(null, "resolved");

        verify(defectRepository).findAllByStatusAndDeletedFalse(Defect.DefectStatus.RESOLVED);
    }

    @Test
    void exportDefectsCsv_noFilters() {
        when(defectRepository.findAllByDeletedFalse()).thenReturn(Collections.emptyList());

        exportService.exportDefectsCsv(null, null);

        verify(defectRepository).findAllByDeletedFalse();
    }

    @Test
    void exportDefectsCsv_withData_producesCorrectCsv() {
        User reporter = User.builder().email("r@test.com").firstName("R").lastName("L").displayName("Reporter").passwordHash("h").build();
        reporter.setId(UUID.randomUUID());
        User assignee = User.builder().email("a@test.com").firstName("A").lastName("B").displayName("Assignee").passwordHash("h").build();
        assignee.setId(UUID.randomUUID());

        Defect defect = Defect.builder()
                .title("Bug #1")
                .severity(Defect.Severity.HIGH)
                .status(Defect.DefectStatus.OPEN)
                .project(project)
                .reportedBy(reporter)
                .assignedTo(assignee)
                .build();
        defect.setId(UUID.randomUUID());
        defect.setCreatedAt(Instant.parse("2026-11-01T10:00:00Z"));
        defect.setUpdatedAt(Instant.parse("2026-11-02T10:00:00Z"));

        when(defectRepository.findAllByDeletedFalse()).thenReturn(List.of(defect));

        String csv = new String(exportService.exportDefectsCsv(null, null), StandardCharsets.UTF_8);
        String[] lines = csv.split("\n");

        assertEquals(2, lines.length);
        assertTrue(lines[0].startsWith("ID,Title,Severity,Status,"));
        assertTrue(lines[1].contains("Bug #1"));
        assertTrue(lines[1].contains("HIGH"));
        assertTrue(lines[1].contains("OPEN"));
        assertTrue(lines[1].contains("Reporter"));
        assertTrue(lines[1].contains("Assignee"));
    }

    @Test
    void exportDefectsCsv_nullFieldsHandled() {
        Defect defect = Defect.builder()
                .title("Bug")
                .severity(Defect.Severity.LOW)
                .status(Defect.DefectStatus.CLOSED)
                .project(null)
                .reportedBy(null)
                .assignedTo(null)
                .build();
        defect.setId(UUID.randomUUID());

        when(defectRepository.findAllByDeletedFalse()).thenReturn(List.of(defect));

        String csv = new String(exportService.exportDefectsCsv(null, null), StandardCharsets.UTF_8);

        assertFalse(csv.isEmpty());
        assertTrue(csv.contains("Bug"));
    }

    @Test
    void exportDefectsCsv_statusCaseInsensitive() {
        when(defectRepository.findAllByStatusAndDeletedFalse(Defect.DefectStatus.IN_PROGRESS))
                .thenReturn(Collections.emptyList());

        exportService.exportDefectsCsv(null, "in_progress");

        verify(defectRepository).findAllByStatusAndDeletedFalse(Defect.DefectStatus.IN_PROGRESS);
    }
}
