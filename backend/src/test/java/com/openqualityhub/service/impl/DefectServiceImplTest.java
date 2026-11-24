package com.openqualityhub.service.impl;

import com.openqualityhub.dto.request.CreateDefectRequest;
import com.openqualityhub.dto.request.UpdateDefectRequest;
import com.openqualityhub.dto.response.DefectResponse;
import com.openqualityhub.entity.Defect;
import com.openqualityhub.entity.Project;
import com.openqualityhub.entity.User;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.mapper.DefectMapper;
import com.openqualityhub.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefectServiceImplTest {

    @Mock private DefectRepository defectRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private ReleaseRepository releaseRepository;
    @Mock private UserRepository userRepository;
    @Mock private DefectMapper defectMapper;

    @InjectMocks
    private DefectServiceImpl defectService;

    private Project project;
    private UUID projectId;
    private UUID defectId;
    private DefectResponse mockResponse;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        defectId = UUID.randomUUID();

        project = Project.builder().name("Test Project").build();
        project.setId(projectId);

        mockResponse = DefectResponse.builder()
            .id(defectId)
            .title("Test Defect")
            .severity("HIGH")
            .status("OPEN")
            .build();
    }

    @Test
    void create_shouldSaveDefectWithCorrectFields() {
        CreateDefectRequest req = CreateDefectRequest.builder()
            .projectId(projectId)
            .title("Login button broken")
            .description("Button does not respond")
            .severity("HIGH")
            .priority(2)
            .component("auth-ui")
            .build();

        when(projectRepository.findByIdAndDeletedFalse(projectId)).thenReturn(Optional.of(project));
        when(defectRepository.save(any(Defect.class))).thenAnswer(inv -> {
            Defect d = inv.getArgument(0);
            d.setId(defectId);
            return d;
        });
        when(defectMapper.toResponse(any(Defect.class))).thenReturn(mockResponse);

        DefectResponse result = defectService.create(req, null);

        assertNotNull(result);
        ArgumentCaptor<Defect> captor = ArgumentCaptor.forClass(Defect.class);
        verify(defectRepository).save(captor.capture());
        Defect saved = captor.getValue();
        assertEquals("Login button broken", saved.getTitle());
        assertEquals(Defect.Severity.HIGH, saved.getSeverity());
        assertEquals(Defect.DefectStatus.OPEN, saved.getStatus());
        assertEquals(project, saved.getProject());
        assertEquals(2, saved.getPriority());
    }

    @Test
    void create_shouldThrowWhenProjectNotFound() {
        CreateDefectRequest req = CreateDefectRequest.builder()
            .projectId(projectId).title("Bug").severity("LOW").build();

        when(projectRepository.findByIdAndDeletedFalse(projectId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> defectService.create(req, null));
    }

    @Test
    void create_shouldSetReportedByWhenProvided() {
        UUID reporterId = UUID.randomUUID();
        User reporter = User.builder().displayName("Reporter").build();
        reporter.setId(reporterId);

        CreateDefectRequest req = CreateDefectRequest.builder()
            .projectId(projectId).title("Bug").severity("LOW").build();

        when(projectRepository.findByIdAndDeletedFalse(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(defectRepository.save(any(Defect.class))).thenAnswer(inv -> {
            Defect d = inv.getArgument(0);
            d.setId(defectId);
            return d;
        });
        when(defectMapper.toResponse(any(Defect.class))).thenReturn(mockResponse);

        defectService.create(req, reporterId);

        ArgumentCaptor<Defect> captor = ArgumentCaptor.forClass(Defect.class);
        verify(defectRepository).save(captor.capture());
        assertEquals(reporter, captor.getValue().getReportedBy());
    }

    @Test
    void update_shouldSetResolvedAtWhenStatusResolved() {
        Defect defect = Defect.builder()
            .title("Existing").severity(Defect.Severity.MEDIUM).status(Defect.DefectStatus.OPEN).build();
        defect.setId(defectId);

        UpdateDefectRequest req = UpdateDefectRequest.builder().status("RESOLVED").build();

        when(defectRepository.findByIdAndDeletedFalse(defectId)).thenReturn(Optional.of(defect));
        when(defectRepository.save(any(Defect.class))).thenReturn(defect);
        when(defectMapper.toResponse(any(Defect.class))).thenReturn(mockResponse);

        defectService.update(defectId, req);

        assertEquals(Defect.DefectStatus.RESOLVED, defect.getStatus());
        assertNotNull(defect.getResolvedAt());
    }

    @Test
    void update_shouldSetResolvedAtWhenStatusClosed() {
        Defect defect = Defect.builder()
            .title("Existing").severity(Defect.Severity.MEDIUM).status(Defect.DefectStatus.OPEN).build();
        defect.setId(defectId);

        UpdateDefectRequest req = UpdateDefectRequest.builder().status("CLOSED").build();

        when(defectRepository.findByIdAndDeletedFalse(defectId)).thenReturn(Optional.of(defect));
        when(defectRepository.save(any(Defect.class))).thenReturn(defect);
        when(defectMapper.toResponse(any(Defect.class))).thenReturn(mockResponse);

        defectService.update(defectId, req);

        assertNotNull(defect.getResolvedAt());
    }

    @Test
    void update_shouldNotSetResolvedAtForOpenStatus() {
        Defect defect = Defect.builder()
            .title("Existing").severity(Defect.Severity.MEDIUM).status(Defect.DefectStatus.OPEN).build();
        defect.setId(defectId);

        UpdateDefectRequest req = UpdateDefectRequest.builder().status("IN_PROGRESS").build();

        when(defectRepository.findByIdAndDeletedFalse(defectId)).thenReturn(Optional.of(defect));
        when(defectRepository.save(any(Defect.class))).thenReturn(defect);
        when(defectMapper.toResponse(any(Defect.class))).thenReturn(mockResponse);

        defectService.update(defectId, req);

        assertNull(defect.getResolvedAt());
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(defectRepository.findByIdAndDeletedFalse(defectId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> defectService.getById(defectId));
    }

    @Test
    void delete_shouldSoftDelete() {
        Defect defect = Defect.builder().title("To delete").severity(Defect.Severity.LOW)
            .status(Defect.DefectStatus.OPEN).build();
        defect.setId(defectId);

        when(defectRepository.findByIdAndDeletedFalse(defectId)).thenReturn(Optional.of(defect));
        when(defectRepository.save(any(Defect.class))).thenReturn(defect);

        defectService.delete(defectId);

        assertTrue(defect.isDeleted());
        verify(defectRepository).save(defect);
    }

    @Test
    void delete_shouldThrowWhenNotFound() {
        when(defectRepository.findByIdAndDeletedFalse(defectId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> defectService.delete(defectId));
    }
}
