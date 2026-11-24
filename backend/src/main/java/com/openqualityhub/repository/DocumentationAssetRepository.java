package com.openqualityhub.repository;

import com.openqualityhub.entity.DocumentationAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentationAssetRepository extends JpaRepository<DocumentationAsset, UUID> {

    Page<DocumentationAsset> findByDeletedFalse(Pageable pageable);

    Optional<DocumentationAsset> findByIdAndDeletedFalse(UUID id);

    Page<DocumentationAsset> findByProjectIdAndDeletedFalse(UUID projectId, Pageable pageable);

    Page<DocumentationAsset> findByHealthStatusAndDeletedFalse(DocumentationAsset.HealthStatus status, Pageable pageable);

    Page<DocumentationAsset> findByDocTypeAndDeletedFalse(DocumentationAsset.DocType docType, Pageable pageable);

    long countByHealthStatusAndDeletedFalse(DocumentationAsset.HealthStatus status);

    long countByDeletedFalse();

    List<DocumentationAsset> findByDeletedFalseAndOwnerIsNull();

    long countByOwnerIsNullAndDeletedFalse();
}
