package com.openqualityhub.service.impl;

import com.openqualityhub.dto.response.TestRunDetailResponse;
import com.openqualityhub.dto.response.TestRunResponse;
import com.openqualityhub.entity.Project;
import com.openqualityhub.entity.TestRun;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.mapper.TestRunMapper;
import com.openqualityhub.repository.ProjectRepository;
import com.openqualityhub.repository.TestRunRepository;
import com.openqualityhub.service.impl.parser.JUnitXmlParser;
import com.openqualityhub.service.impl.parser.PlaywrightJsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestRunServiceImplTest {

    @Mock private TestRunRepository testRunRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private TestRunMapper testRunMapper;
    @Mock private JUnitXmlParser junitParser;
    @Mock private PlaywrightJsonParser playwrightParser;
    @Mock private TestIdentityService testIdentityService;

    @InjectMocks
    private TestRunServiceImpl testRunService;

    private UUID testRunId;
    private UUID projectId;
    private Project project;
    private TestRun testRun;
    private TestRunResponse mockResponse;
    private TestRunDetailResponse mockDetailResponse;

    @BeforeEach
    void setUp() {
        testRunId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        project = Project.builder().name("Test Project").build();
        project.setId(projectId);

        testRun = TestRun.builder()
            .name("Nightly Suite")
            .source(TestRun.TestSource.JUNIT)
            .environment("staging")
            .totalTests(100)
            .passed(95)
            .failed(3)
            .skipped(2)
            .durationMs(5000L)
            .executedAt(Instant.now())
            .project(project)
            .build();
        testRun.setId(testRunId);

        mockResponse = new TestRunResponse();
        mockResponse.setId(testRunId);
        mockResponse.setName("Nightly Suite");
        mockResponse.setSource(TestRun.TestSource.JUNIT);
        mockResponse.setTotalTests(100);
        mockResponse.setPassed(95);
        mockResponse.setFailed(3);
        mockResponse.setSkipped(2);
        mockResponse.setProjectId(projectId);
        mockResponse.setProjectName("Test Project");

        mockDetailResponse = new TestRunDetailResponse();
        mockDetailResponse.setId(testRunId);
        mockDetailResponse.setName("Nightly Suite");
        mockDetailResponse.setSource(TestRun.TestSource.JUNIT);
        mockDetailResponse.setTotalTests(100);
        mockDetailResponse.setPassed(95);
    }

    @Test
    void importJUnitResults_shouldParseAndSave() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("<testsuites/>".getBytes()));

        TestRun parsedRun = TestRun.builder()
            .totalTests(50)
            .passed(48)
            .failed(2)
            .skipped(0)
            .build();

        when(projectRepository.findByIdAndDeletedFalse(projectId)).thenReturn(Optional.of(project));
        when(junitParser.parse(any())).thenReturn(parsedRun);
        when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
            TestRun tr = inv.getArgument(0);
            tr.setId(testRunId);
            return tr;
        });
        when(testRunMapper.toResponse(any(TestRun.class))).thenReturn(mockResponse);

        TestRunResponse result = testRunService.importJUnitResults(projectId, "CI Run", "staging", file);

        assertNotNull(result);
        ArgumentCaptor<TestRun> captor = ArgumentCaptor.forClass(TestRun.class);
        verify(testRunRepository).save(captor.capture());
        TestRun saved = captor.getValue();
        assertEquals("CI Run", saved.getName());
        assertEquals("staging", saved.getEnvironment());
        assertEquals(project, saved.getProject());
        assertEquals(TestRun.TestSource.JUNIT, saved.getSource());
        verify(testIdentityService).recordExecutions(any(TestRun.class));
    }

    @Test
    void importJUnitResults_shouldThrowWhenProjectNotFound() {
        MultipartFile file = mock(MultipartFile.class);
        when(projectRepository.findByIdAndDeletedFalse(projectId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> testRunService.importJUnitResults(projectId, "Run", "env", file));
    }

    @Test
    void importJUnitResults_shouldThrowOnParseError() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("bad xml".getBytes()));

        when(projectRepository.findByIdAndDeletedFalse(projectId)).thenReturn(Optional.of(project));
        when(junitParser.parse(any())).thenThrow(new RuntimeException("Parse error"));

        assertThrows(IllegalArgumentException.class,
            () -> testRunService.importJUnitResults(projectId, "Run", "env", file));
    }

    @Test
    void getById_shouldUseSoftDeleteAwareQuery() {
        when(testRunRepository.findByIdAndDeletedFalse(testRunId)).thenReturn(Optional.of(testRun));
        when(testRunMapper.toDetailResponse(testRun)).thenReturn(mockDetailResponse);

        TestRunDetailResponse result = testRunService.getById(testRunId);

        assertNotNull(result);
        assertEquals("Nightly Suite", result.getName());
        verify(testRunRepository).findByIdAndDeletedFalse(testRunId);
        verify(testRunRepository, never()).findById(any());
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(testRunRepository.findByIdAndDeletedFalse(testRunId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> testRunService.getById(testRunId));
    }

    @Test
    void getByProjectId_shouldReturnPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TestRun> page = new PageImpl<>(List.of(testRun), pageable, 1);

        when(projectRepository.findByIdAndDeletedFalse(projectId)).thenReturn(Optional.of(project));
        when(testRunRepository.findByProjectIdAndDeletedFalseOrderByExecutedAtDesc(projectId, pageable))
            .thenReturn(page);
        when(testRunMapper.toResponse(testRun)).thenReturn(mockResponse);

        Page<TestRunResponse> result = testRunService.getByProjectId(projectId, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Nightly Suite", result.getContent().get(0).getName());
        verify(testRunRepository).findByProjectIdAndDeletedFalseOrderByExecutedAtDesc(projectId, pageable);
    }

    @Test
    void getByProjectId_shouldThrowWhenProjectNotFound() {
        Pageable pageable = PageRequest.of(0, 10);
        when(projectRepository.findByIdAndDeletedFalse(projectId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> testRunService.getByProjectId(projectId, pageable));
    }

    @Test
    void delete_shouldSoftDelete() {
        when(testRunRepository.findByIdAndDeletedFalse(testRunId)).thenReturn(Optional.of(testRun));
        when(testRunRepository.save(any(TestRun.class))).thenReturn(testRun);

        testRunService.delete(testRunId);

        assertTrue(testRun.isDeleted());
        verify(testRunRepository).save(testRun);
    }

    @Test
    void delete_shouldThrowWhenNotFound() {
        when(testRunRepository.findByIdAndDeletedFalse(testRunId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> testRunService.delete(testRunId));
    }

    @Test
    void getAll_shouldReturnPaginated() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<TestRun> page = new PageImpl<>(List.of(testRun), pageable, 1);

        when(testRunRepository.findByDeletedFalse(pageable)).thenReturn(page);
        when(testRunMapper.toResponse(testRun)).thenReturn(mockResponse);

        Page<TestRunResponse> result = testRunService.getAll(pageable);

        assertEquals(1, result.getTotalElements());
        verify(testRunRepository).findByDeletedFalse(pageable);
    }

    @Test
    void importPlaywrightResults_shouldParseAndSave() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("{}".getBytes()));

        TestRun parsedRun = TestRun.builder()
            .totalTests(20)
            .passed(18)
            .failed(2)
            .skipped(0)
            .build();

        when(projectRepository.findByIdAndDeletedFalse(projectId)).thenReturn(Optional.of(project));
        when(playwrightParser.parse(any())).thenReturn(parsedRun);
        when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
            TestRun tr = inv.getArgument(0);
            tr.setId(testRunId);
            return tr;
        });
        when(testRunMapper.toResponse(any(TestRun.class))).thenReturn(mockResponse);

        TestRunResponse result = testRunService.importPlaywrightResults(projectId, "PW Run", "prod", file);

        assertNotNull(result);
        ArgumentCaptor<TestRun> captor = ArgumentCaptor.forClass(TestRun.class);
        verify(testRunRepository).save(captor.capture());
        assertEquals(TestRun.TestSource.PLAYWRIGHT, captor.getValue().getSource());
        assertEquals("PW Run", captor.getValue().getName());
        verify(testIdentityService).recordExecutions(any(TestRun.class));
    }
}
