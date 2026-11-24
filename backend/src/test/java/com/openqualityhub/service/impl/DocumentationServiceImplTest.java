package com.openqualityhub.service.impl;

import com.openqualityhub.dto.request.CreateDocumentationAssetRequest;
import com.openqualityhub.dto.response.DocumentationAssetResponse;
import com.openqualityhub.dto.response.DocumentationDashboardResponse;
import com.openqualityhub.entity.DocumentationAsset;
import com.openqualityhub.entity.DocumentationReview;
import com.openqualityhub.entity.Organization;
import com.openqualityhub.entity.Project;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.mapper.DocumentationAssetMapper;
import com.openqualityhub.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentationServiceImplTest {

    private static final UUID DEFAULT_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock private DocumentationAssetRepository assetRepository;
    @Mock private DocumentationCheckRepository checkRepository;
    @Mock private DocumentationReviewRepository reviewRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private DocumentationAssetMapper mapper;

    @InjectMocks
    private DocumentationServiceImpl documentationService;

    private UUID assetId;
    private UUID projectId;
    private Organization organization;
    private Project project;
    private DocumentationAsset asset;
    private DocumentationAssetResponse mockResponse;

    @BeforeEach
    void setUp() {
        assetId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        organization = Organization.builder().name("Test Org").slug("test-org").build();
        organization.setId(DEFAULT_ORG_ID);

        project = Project.builder().name("Test Project").build();
        project.setId(projectId);

        asset = DocumentationAsset.builder()
            .organization(organization)
            .title("API Spec")
            .description("Main API specification")
            .docType(DocumentationAsset.DocType.API_SPEC)
            .url("https://docs.example.com/api")
            .category(DocumentationAsset.Category.TECHNICAL)
            .healthStatus(DocumentationAsset.HealthStatus.UNKNOWN)
            .reviewCycleDays(90)
            .build();
        asset.setId(assetId);

        mockResponse = DocumentationAssetResponse.builder()
            .id(assetId.toString())
            .title("API Spec")
            .docType("API_SPEC")
            .category("TECHNICAL")
            .healthStatus("UNKNOWN")
            .build();
    }

    @Test
    void create_shouldSaveAsset() {
        CreateDocumentationAssetRequest request = CreateDocumentationAssetRequest.builder()
            .title("API Spec")
            .description("Main API specification")
            .docType("API_SPEC")
            .url("https://docs.example.com/api")
            .category("TECHNICAL")
            .build();

        when(organizationRepository.findById(DEFAULT_ORG_ID)).thenReturn(Optional.of(organization));
        when(assetRepository.save(any(DocumentationAsset.class))).thenAnswer(inv -> {
            DocumentationAsset a = inv.getArgument(0);
            a.setId(assetId);
            return a;
        });
        when(mapper.toResponse(any(DocumentationAsset.class))).thenReturn(mockResponse);

        DocumentationAssetResponse result = documentationService.create(request);

        assertNotNull(result);
        assertEquals("API Spec", result.getTitle());
        ArgumentCaptor<DocumentationAsset> captor = ArgumentCaptor.forClass(DocumentationAsset.class);
        verify(assetRepository).save(captor.capture());
        DocumentationAsset saved = captor.getValue();
        assertEquals("API Spec", saved.getTitle());
        assertEquals(DocumentationAsset.DocType.API_SPEC, saved.getDocType());
        assertEquals(DocumentationAsset.HealthStatus.UNKNOWN, saved.getHealthStatus());
        assertEquals(organization, saved.getOrganization());
    }

    @Test
    void create_shouldThrowWhenNoUrlOrRepoPath() {
        CreateDocumentationAssetRequest request = CreateDocumentationAssetRequest.builder()
            .title("Empty Doc")
            .docType("API_SPEC")
            .category("TECHNICAL")
            .build();

        assertThrows(IllegalArgumentException.class, () -> documentationService.create(request));
    }

    @Test
    void create_shouldSetProjectWhenProvided() {
        CreateDocumentationAssetRequest request = CreateDocumentationAssetRequest.builder()
            .title("API Spec")
            .docType("API_SPEC")
            .url("https://example.com")
            .category("TECHNICAL")
            .projectId(projectId)
            .build();

        when(organizationRepository.findById(DEFAULT_ORG_ID)).thenReturn(Optional.of(organization));
        when(projectRepository.findByIdAndDeletedFalse(projectId)).thenReturn(Optional.of(project));
        when(assetRepository.save(any(DocumentationAsset.class))).thenAnswer(inv -> {
            DocumentationAsset a = inv.getArgument(0);
            a.setId(assetId);
            return a;
        });
        when(mapper.toResponse(any(DocumentationAsset.class))).thenReturn(mockResponse);

        documentationService.create(request);

        ArgumentCaptor<DocumentationAsset> captor = ArgumentCaptor.forClass(DocumentationAsset.class);
        verify(assetRepository).save(captor.capture());
        assertEquals(project, captor.getValue().getProject());
    }

    @Test
    void getById_shouldUseSoftDeleteAwareQuery() {
        when(assetRepository.findByIdAndDeletedFalse(assetId)).thenReturn(Optional.of(asset));
        when(mapper.toResponse(asset)).thenReturn(mockResponse);

        DocumentationAssetResponse result = documentationService.getById(assetId);

        assertNotNull(result);
        verify(assetRepository).findByIdAndDeletedFalse(assetId);
        verify(assetRepository, never()).findById(any());
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(assetRepository.findByIdAndDeletedFalse(assetId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> documentationService.getById(assetId));
    }

    @Test
    void update_shouldUpdateFields() {
        CreateDocumentationAssetRequest request = CreateDocumentationAssetRequest.builder()
            .title("Updated Title")
            .description("Updated description")
            .docType("USER_GUIDE")
            .category("USER_FACING")
            .build();

        when(assetRepository.findByIdAndDeletedFalse(assetId)).thenReturn(Optional.of(asset));
        when(assetRepository.save(any(DocumentationAsset.class))).thenReturn(asset);
        when(mapper.toResponse(any(DocumentationAsset.class))).thenReturn(mockResponse);

        documentationService.update(assetId, request);

        assertEquals("Updated Title", asset.getTitle());
        assertEquals("Updated description", asset.getDescription());
        assertEquals(DocumentationAsset.DocType.USER_GUIDE, asset.getDocType());
        assertEquals(DocumentationAsset.Category.USER_FACING, asset.getCategory());
        verify(assetRepository).save(asset);
    }

    @Test
    void delete_shouldSoftDelete() {
        when(assetRepository.findByIdAndDeletedFalse(assetId)).thenReturn(Optional.of(asset));
        when(assetRepository.save(any(DocumentationAsset.class))).thenReturn(asset);

        documentationService.delete(assetId);

        assertTrue(asset.isDeleted());
        verify(assetRepository).save(asset);
    }

    @Test
    void delete_shouldThrowWhenNotFound() {
        when(assetRepository.findByIdAndDeletedFalse(assetId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> documentationService.delete(assetId));
    }

    @Test
    void getDashboard_shouldReturnStats() {
        when(assetRepository.countByDeletedFalse()).thenReturn(15L);
        when(assetRepository.countByHealthStatusAndDeletedFalse(DocumentationAsset.HealthStatus.HEALTHY)).thenReturn(8L);
        when(assetRepository.countByHealthStatusAndDeletedFalse(DocumentationAsset.HealthStatus.WARNING)).thenReturn(4L);
        when(assetRepository.countByHealthStatusAndDeletedFalse(DocumentationAsset.HealthStatus.CRITICAL)).thenReturn(2L);
        when(assetRepository.countByHealthStatusAndDeletedFalse(DocumentationAsset.HealthStatus.UNKNOWN)).thenReturn(1L);
        when(assetRepository.countByOwnerIsNullAndDeletedFalse()).thenReturn(3L);
        when(reviewRepository.countByStatusAndDeletedFalse(DocumentationReview.ReviewStatus.PENDING)).thenReturn(5L);

        // Mock the byType calls (getDashboard iterates over all DocType values)
        // Use lenient() because the service filters by count > 0, making some stubs appear unused to strict Mockito
        for (DocumentationAsset.DocType type : DocumentationAsset.DocType.values()) {
            Page<DocumentationAsset> page;
            if (type == DocumentationAsset.DocType.API_SPEC) {
                page = new PageImpl<>(List.of(asset), Pageable.unpaged(), 3);
            } else {
                page = new PageImpl<>(List.of());
            }
            Mockito.lenient().when(assetRepository.findByDocTypeAndDeletedFalse(eq(type), any(Pageable.class))).thenReturn(page);
        }

        DocumentationDashboardResponse result = documentationService.getDashboard();

        assertNotNull(result);
        assertEquals(15, result.getTotalAssets());
        assertEquals(8, result.getHealthyCount());
        assertEquals(4, result.getWarningCount());
        assertEquals(2, result.getCriticalCount());
        assertEquals(1, result.getUnknownCount());
        assertEquals(3, result.getMissingOwnerCount());
        assertEquals(5, result.getPendingReviews());
        assertFalse(result.getAssetsByType().isEmpty());
    }

    @Test
    void getAll_shouldReturnPaginatedResults() {
        Pageable pageable = Pageable.unpaged();
        Page<DocumentationAsset> page = new PageImpl<>(List.of(asset), pageable, 1);

        when(assetRepository.findByDeletedFalse(pageable)).thenReturn(page);
        when(mapper.toResponse(asset)).thenReturn(mockResponse);

        Page<DocumentationAssetResponse> result = documentationService.getAll(pageable);

        assertEquals(1, result.getTotalElements());
        verify(assetRepository).findByDeletedFalse(pageable);
    }
}
