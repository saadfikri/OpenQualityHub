package com.openqualityhub.repository;

import com.openqualityhub.entity.DocumentationCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentationCheckRepository extends JpaRepository<DocumentationCheck, UUID> {

    List<DocumentationCheck> findByAssetIdOrderByExecutedAtDesc(UUID assetId);

    long countByAssetIdAndStatus(UUID assetId, DocumentationCheck.CheckStatus status);
}
