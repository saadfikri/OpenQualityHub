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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayOutputStream;
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

    private String exportTestRuns(UUID pid) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportTestRunsCsv(pid, out);
        return out.toString(StandardCharsets.UTF_8);
    }

    private String exportDefects(UUID pid, String status) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportDefectsCsv(pid, status, out);
        return out.toString(StandardCharsets.UTF_8);
    }

    // --- exportTestRunsCsv ---

    @Test
    void exportTestRunsCsv_withProjectId_filtersByProject() {
        Page<TestRun> empty = new PageImpl<>(Collections.emptyList());
        when(testRunRepository.findByProjectIdAndDeletedFalseOrderByExecutedAtDesc(eq(projectId), any(Pageable.class)))
                .thenReturn(empty);

        String csv = exportTestRuns(projectId);

        assertTrue(csv.startsWith("ID,Name,Source,Project,"));
        verify(testRunRepository).findByProjectIdAndDeletedFalseOrderByExecutedAtDesc(eq(projectId), any(Pageable.class));
    }

    @Test
    void exportTestRunsCsv_withoutProjectId_getsAll() {
        Page<TestRun> empty = new PageImpl<>(Collections.emptyList());
        when(testRunRepository.findByDeletedFalse(any(Pageable.class))).thenReturn(empty);

        String csv = exportTestRuns(null);

        assertNotNull(csv);
        verify(testRunRepository).findByDeletedFalse(any(Pageable.class));
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

        Page<TestRun> page = new PageImpl<>(List.of(run));
        when(testRunRepository.findByDeletedFalse(any(Pageable.class))).thenReturn(page);

        String csv = exportTestRuns(null);
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

        Page<TestRun> page = new PageImpl<>(List.of(run));
        when(testRunRepository.findByDeletedFalse(any(Pageable.class))).thenReturn(page);

        String csv = exportTestRuns(null);

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

        Page<TestRun> page = new PageImpl<>(List.of(run));
        when(testRunRepository.findByDeletedFalse(any(Pageable.class))).thenReturn(page);

        String csv = exportTestRuns(null);

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

        Page<TestRun> page = new PageImpl<>(List.of(run));
        when(testRunRepository.findByDeletedFalse(any(Pageable.class))).thenReturn(page);

        String csv = exportTestRuns(null);

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

        Page<TestRun> page = new PageImpl<>(List.of(run));
        when(testRunRepository.findByDeletedFalse(any(Pageable.class))).thenReturn(page);

        String csv = exportTestRuns(null);

        assertTrue(csv.contains("\"line1\nline2\""));
    }

    // --- exportDefectsCsv ---

    @Test
    void exportDefectsCsv_bothProjectIdAndStatus() {
        Page<Defect> empty = new PageImpl<>(Collections.emptyList());
        when(defectRepository.findByProjectIdAndStatusAndDeletedFalse(eq(projectId), eq(Defect.DefectStatus.OPEN), any(Pageable.class)))
                .thenReturn(empty);

        String csv = exportDefects(projectId, "open");

        assertNotNull(csv);
        verify(defectRepository).findByProjectIdAndStatusAndDeletedFalse(eq(projectId), eq(Defect.DefectStatus.OPEN), any(Pageable.class));
    }

    @Test
    void exportDefectsCsv_onlyProjectId() {
        Page<Defect> empty = new PageImpl<>(Collections.emptyList());
        when(defectRepository.findByProjectIdAndDeletedFalse(eq(projectId), any(Pageable.class)))
                .thenReturn(empty);

        exportDefects(projectId, null);

        verify(defectRepository).findByProjectIdAndDeletedFalse(eq(projectId), any(Pageable.class));
    }

    @Test
    void exportDefectsCsv_onlyStatus() {
        Page<Defect> empty = new PageImpl<>(Collections.emptyList());
        when(defectRepository.findByStatusAndDeletedFalse(eq(Defect.DefectStatus.RESOLVED), any(Pageable.class)))
                .thenReturn(empty);

        exportDefects(null, "resolved");

        verify(defectRepository).findByStatusAndDeletedFalse(eq(Defect.DefectStatus.RESOLVED), any(Pageable.class));
    }

    @Test
    void exportDefectsCsv_noFilters() {
        Page<Defect> empty = new PageImpl<>(Collections.emptyList());
        when(defectRepository.findByDeletedFalse(any(Pageable.class))).thenReturn(empty);

        exportDefects(null, null);

        verify(defectRepository).findByDeletedFalse(any(Pageable.class));
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

        Page<Defect> page = new PageImpl<>(List.of(defect));
        when(defectRepository.findByDeletedFalse(any(Pageable.class))).thenReturn(page);

        String csv = exportDefects(null, null);
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

        Page<Defect> page = new PageImpl<>(List.of(defect));
        when(defectRepository.findByDeletedFalse(any(Pageable.class))).thenReturn(page);

        String csv = exportDefects(null, null);

        assertFalse(csv.isEmpty());
        assertTrue(csv.contains("Bug"));
    }

    @Test
    void exportDefectsCsv_statusCaseInsensitive() {
        Page<Defect> empty = new PageImpl<>(Collections.emptyList());
        when(defectRepository.findByStatusAndDeletedFalse(eq(Defect.DefectStatus.IN_PROGRESS), any(Pageable.class)))
                .thenReturn(empty);

        exportDefects(null, "in_progress");

        verify(defectRepository).findByStatusAndDeletedFalse(eq(Defect.DefectStatus.IN_PROGRESS), any(Pageable.class));
    }
}
