package com.openqualityhub.mapper;

import com.openqualityhub.dto.response.DocumentationAssetResponse;
import com.openqualityhub.entity.DocumentationAsset;
import com.openqualityhub.entity.DocumentationCheck;
import com.openqualityhub.entity.DocumentationReview;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocumentationAssetMapper {

    @Mapping(target = "id", expression = "java(a.getId() != null ? a.getId().toString() : null)")
    @Mapping(target = "docType", expression = "java(a.getDocType() != null ? a.getDocType().name() : null)")
    @Mapping(target = "category", expression = "java(a.getCategory() != null ? a.getCategory().name() : null)")
    @Mapping(target = "healthStatus", expression = "java(a.getHealthStatus() != null ? a.getHealthStatus().name() : null)")
    @Mapping(source = "owner.id", target = "ownerId")
    @Mapping(source = "owner.displayName", target = "ownerName")
    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "project.name", target = "projectName")
    @Mapping(target = "lastCheckedAt", expression = "java(a.getLastCheckedAt() != null ? a.getLastCheckedAt().toString() : null)")
    @Mapping(target = "lastReviewedAt", expression = "java(a.getLastReviewedAt() != null ? a.getLastReviewedAt().toString() : null)")
    @Mapping(target = "createdAt", expression = "java(a.getCreatedAt() != null ? a.getCreatedAt().toString() : null)")
    @Mapping(target = "updatedAt", expression = "java(a.getUpdatedAt() != null ? a.getUpdatedAt().toString() : null)")
    @Mapping(source = "checks", target = "recentChecks")
    @Mapping(source = "reviews", target = "recentReviews")
    DocumentationAssetResponse toResponse(DocumentationAsset a);

    @Mapping(target = "id", expression = "java(c.getId() != null ? c.getId().toString() : null)")
    @Mapping(target = "checkType", expression = "java(c.getCheckType() != null ? c.getCheckType().name() : null)")
    @Mapping(target = "status", expression = "java(c.getStatus() != null ? c.getStatus().name() : null)")
    @Mapping(target = "executedAt", expression = "java(c.getExecutedAt() != null ? c.getExecutedAt().toString() : null)")
    DocumentationAssetResponse.CheckResponse toCheckResponse(DocumentationCheck c);

    List<DocumentationAssetResponse.CheckResponse> toCheckResponses(List<DocumentationCheck> checks);

    @Mapping(target = "id", expression = "java(r.getId() != null ? r.getId().toString() : null)")
    @Mapping(source = "reviewer.id", target = "reviewerId")
    @Mapping(source = "reviewer.displayName", target = "reviewerName")
    @Mapping(target = "status", expression = "java(r.getStatus() != null ? r.getStatus().name() : null)")
    @Mapping(target = "dueDate", expression = "java(r.getDueDate() != null ? r.getDueDate().toString() : null)")
    @Mapping(target = "completedAt", expression = "java(r.getCompletedAt() != null ? r.getCompletedAt().toString() : null)")
    @Mapping(target = "createdAt", expression = "java(r.getCreatedAt() != null ? r.getCreatedAt().toString() : null)")
    DocumentationAssetResponse.ReviewResponse toReviewResponse(DocumentationReview r);

    List<DocumentationAssetResponse.ReviewResponse> toReviewResponses(List<DocumentationReview> reviews);
}
