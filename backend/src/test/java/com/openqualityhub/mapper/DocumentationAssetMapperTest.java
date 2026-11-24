package com.openqualityhub.mapper;

import com.openqualityhub.dto.response.DocumentationAssetResponse;
import com.openqualityhub.entity.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DocumentationAssetMapperTest {

    private final DocumentationAssetMapper mapper = Mappers.getMapper(DocumentationAssetMapper.class);

    @Test
    void toResponse_shouldMapAllFields() {
        User owner = User.builder().displayName("Doc Owner").build();
        owner.setId(UUID.randomUUID());

        Project project = Project.builder().name("Core API").build();
        project.setId(UUID.randomUUID());

        DocumentationAsset asset = DocumentationAsset.builder()
            .title("API Specification")
            .description("OpenAPI spec for Core API")
            .docType(DocumentationAsset.DocType.API_SPEC)
            .url("https://docs.example.com/api")
            .repoPath("docs/api/openapi.yaml")
            .category(DocumentationAsset.Category.TECHNICAL)
            .owner(owner)
            .project(project)
            .healthStatus(DocumentationAsset.HealthStatus.HEALTHY)
            .lastCheckedAt(Instant.parse("2024-09-01T10:00:00Z"))
            .lastReviewedAt(Instant.parse("2024-08-15T14:00:00Z"))
            .reviewCycleDays(30)
            .checks(new ArrayList<>())
            .reviews(new ArrayList<>())
            .build();
        asset.setId(UUID.randomUUID());
        asset.setCreatedAt(Instant.parse("2024-01-15T08:00:00Z"));
        asset.setUpdatedAt(Instant.parse("2024-09-01T10:00:00Z"));

        DocumentationAssetResponse response = mapper.toResponse(asset);

        assertEquals(asset.getId().toString(), response.getId());
        assertEquals("API Specification", response.getTitle());
        assertEquals("OpenAPI spec for Core API", response.getDescription());
        assertEquals("API_SPEC", response.getDocType());
        assertEquals("https://docs.example.com/api", response.getUrl());
        assertEquals("docs/api/openapi.yaml", response.getRepoPath());
        assertEquals("TECHNICAL", response.getCategory());
        assertEquals(owner.getId().toString(), response.getOwnerId());
        assertEquals("Doc Owner", response.getOwnerName());
        assertEquals(project.getId().toString(), response.getProjectId());
        assertEquals("Core API", response.getProjectName());
        assertEquals("HEALTHY", response.getHealthStatus());
        assertEquals("2024-09-01T10:00:00Z", response.getLastCheckedAt());
        assertEquals("2024-08-15T14:00:00Z", response.getLastReviewedAt());
        assertEquals(30, response.getReviewCycleDays());
        assertEquals("2024-01-15T08:00:00Z", response.getCreatedAt());
        assertEquals("2024-09-01T10:00:00Z", response.getUpdatedAt());
    }

    @Test
    void toResponse_shouldHandleNullOwnerAndProject() {
        DocumentationAsset asset = DocumentationAsset.builder()
            .title("Wiki Page")
            .docType(DocumentationAsset.DocType.WIKI)
            .category(DocumentationAsset.Category.INTERNAL)
            .owner(null)
            .project(null)
            .healthStatus(DocumentationAsset.HealthStatus.UNKNOWN)
            .checks(new ArrayList<>())
            .reviews(new ArrayList<>())
            .build();
        asset.setId(UUID.randomUUID());

        DocumentationAssetResponse response = mapper.toResponse(asset);

        assertNull(response.getOwnerId());
        assertNull(response.getOwnerName());
        assertNull(response.getProjectId());
        assertNull(response.getProjectName());
    }

    @Test
    void toCheckResponse_shouldMapFields() {
        DocumentationCheck check = DocumentationCheck.builder()
            .checkType(DocumentationCheck.CheckType.BROKEN_LINK)
            .status(DocumentationCheck.CheckStatus.FAIL)
            .message("Link to /api/v2 returned 404")
            .executedAt(Instant.parse("2024-09-01T10:30:00Z"))
            .build();
        check.setId(UUID.randomUUID());

        DocumentationAssetResponse.CheckResponse response = mapper.toCheckResponse(check);

        assertEquals(check.getId().toString(), response.getId());
        assertEquals("BROKEN_LINK", response.getCheckType());
        assertEquals("FAIL", response.getStatus());
        assertEquals("Link to /api/v2 returned 404", response.getMessage());
        assertEquals("2024-09-01T10:30:00Z", response.getExecutedAt());
    }

    @Test
    void toReviewResponse_shouldMapFields() {
        User reviewer = User.builder().displayName("Reviewer Jane").build();
        reviewer.setId(UUID.randomUUID());

        DocumentationReview review = DocumentationReview.builder()
            .reviewer(reviewer)
            .status(DocumentationReview.ReviewStatus.APPROVED)
            .dueDate(LocalDate.of(2024, 10, 1))
            .completedAt(Instant.parse("2024-09-28T16:00:00Z"))
            .comment("Looks good, approved")
            .build();
        review.setId(UUID.randomUUID());
        review.setCreatedAt(Instant.parse("2024-09-15T08:00:00Z"));

        DocumentationAssetResponse.ReviewResponse response = mapper.toReviewResponse(review);

        assertEquals(review.getId().toString(), response.getId());
        assertEquals(reviewer.getId().toString(), response.getReviewerId());
        assertEquals("Reviewer Jane", response.getReviewerName());
        assertEquals("APPROVED", response.getStatus());
        assertEquals("2024-10-01", response.getDueDate());
        assertEquals("2024-09-28T16:00:00Z", response.getCompletedAt());
        assertEquals("Looks good, approved", response.getComment());
        assertEquals("2024-09-15T08:00:00Z", response.getCreatedAt());
    }

    @Test
    void toReviewResponse_shouldHandleNullReviewer() {
        DocumentationReview review = DocumentationReview.builder()
            .reviewer(null)
            .status(DocumentationReview.ReviewStatus.PENDING)
            .build();
        review.setId(UUID.randomUUID());

        DocumentationAssetResponse.ReviewResponse response = mapper.toReviewResponse(review);

        assertNull(response.getReviewerId());
        assertNull(response.getReviewerName());
    }

    @Test
    void toResponse_shouldReturnNullForNullInput() {
        assertNull(mapper.toResponse(null));
    }
}
