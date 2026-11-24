package com.openqualityhub.repository;

import com.openqualityhub.entity.DocumentationReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentationReviewRepository extends JpaRepository<DocumentationReview, UUID> {

    Page<DocumentationReview> findByAssetIdAndDeletedFalse(UUID assetId, Pageable pageable);

    Page<DocumentationReview> findByReviewerIdAndDeletedFalse(UUID reviewerId, Pageable pageable);

    Page<DocumentationReview> findByStatusAndDeletedFalse(DocumentationReview.ReviewStatus status, Pageable pageable);

    Optional<DocumentationReview> findByIdAndDeletedFalse(UUID id);

    long countByStatusAndDeletedFalse(DocumentationReview.ReviewStatus status);
}
