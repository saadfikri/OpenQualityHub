package com.openqualityhub.service.impl;

import com.openqualityhub.dto.response.ReleaseResponse;
import com.openqualityhub.entity.Project;
import com.openqualityhub.entity.Release;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.mapper.ReleaseMapper;
import com.openqualityhub.repository.ReleaseRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReleaseServiceImplTest {

    @Mock private ReleaseRepository releaseRepository;
    @Mock private ReleaseMapper releaseMapper;

    @InjectMocks
    private ReleaseServiceImpl releaseService;

    private UUID releaseId;
    private UUID projectId;
    private Project project;
    private Release release;
    private ReleaseResponse mockResponse;

    @BeforeEach
    void setUp() {
        releaseId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        project = Project.builder().name("Test Project").build();
        project.setId(projectId);

        release = Release.builder()
            .releaseVersion("1.0.0")
            .name("Initial Release")
            .description("First release")
            .status(Release.ReleaseStatus.PLANNING)
            .targetDate(LocalDate.of(2026, 12, 1))
            .project(project)
            .build();
        release.setId(releaseId);

        mockResponse = ReleaseResponse.builder()
            .id(releaseId)
            .releaseVersion("1.0.0")
            .name("Initial Release")
            .status("PLANNING")
            .projectId(projectId)
            .projectName("Test Project")
            .build();
    }

    @Test
    void getById_shouldUseSoftDeleteAwareQuery() {
        when(releaseRepository.findByIdAndDeletedFalse(releaseId)).thenReturn(Optional.of(release));
        when(releaseMapper.toResponse(release)).thenReturn(mockResponse);

        ReleaseResponse result = releaseService.getById(releaseId);

        assertNotNull(result);
        assertEquals("1.0.0", result.getReleaseVersion());
        assertEquals("Initial Release", result.getName());
        verify(releaseRepository).findByIdAndDeletedFalse(releaseId);
        verify(releaseRepository, never()).findById(any());
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(releaseRepository.findByIdAndDeletedFalse(releaseId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> releaseService.getById(releaseId));
    }

    @Test
    void getById_shouldReturnMappedResponse() {
        when(releaseRepository.findByIdAndDeletedFalse(releaseId)).thenReturn(Optional.of(release));
        when(releaseMapper.toResponse(release)).thenReturn(mockResponse);

        ReleaseResponse result = releaseService.getById(releaseId);

        assertEquals(releaseId, result.getId());
        assertEquals(projectId, result.getProjectId());
        assertEquals("Test Project", result.getProjectName());
    }

    @Test
    void getByProjectId_shouldReturnPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Release> releasePage = new PageImpl<>(List.of(release), pageable, 1);

        when(releaseRepository.findByProjectIdAndDeletedFalse(projectId, pageable)).thenReturn(releasePage);
        when(releaseMapper.toResponse(release)).thenReturn(mockResponse);

        Page<ReleaseResponse> result = releaseService.getByProjectId(projectId, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("1.0.0", result.getContent().get(0).getReleaseVersion());
        verify(releaseRepository).findByProjectIdAndDeletedFalse(projectId, pageable);
    }

    @Test
    void getByProjectId_shouldReturnEmptyPageWhenNoReleases() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Release> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(releaseRepository.findByProjectIdAndDeletedFalse(projectId, pageable)).thenReturn(emptyPage);

        Page<ReleaseResponse> result = releaseService.getByProjectId(projectId, pageable);

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void getAll_shouldReturnPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 20);
        Release release2 = Release.builder()
            .releaseVersion("2.0.0")
            .name("Second Release")
            .project(project)
            .build();
        release2.setId(UUID.randomUUID());

        ReleaseResponse response2 = ReleaseResponse.builder()
            .id(release2.getId())
            .releaseVersion("2.0.0")
            .name("Second Release")
            .build();

        Page<Release> releasePage = new PageImpl<>(List.of(release, release2), pageable, 2);

        when(releaseRepository.findByDeletedFalse(pageable)).thenReturn(releasePage);
        when(releaseMapper.toResponse(release)).thenReturn(mockResponse);
        when(releaseMapper.toResponse(release2)).thenReturn(response2);

        Page<ReleaseResponse> result = releaseService.getAll(pageable);

        assertEquals(2, result.getTotalElements());
        verify(releaseRepository).findByDeletedFalse(pageable);
    }

    @Test
    void getAll_shouldReturnEmptyPageWhenNoReleases() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Release> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(releaseRepository.findByDeletedFalse(pageable)).thenReturn(emptyPage);

        Page<ReleaseResponse> result = releaseService.getAll(pageable);

        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getAll_shouldUseSoftDeleteAwareQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Release> page = new PageImpl<>(List.of(release), pageable, 1);

        when(releaseRepository.findByDeletedFalse(pageable)).thenReturn(page);
        when(releaseMapper.toResponse(any(Release.class))).thenReturn(mockResponse);

        releaseService.getAll(pageable);

        verify(releaseRepository).findByDeletedFalse(pageable);
        verify(releaseRepository, never()).findAll(pageable);
    }
}
