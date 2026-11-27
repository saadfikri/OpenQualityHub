package com.openqualityhub.repository;

import com.openqualityhub.entity.QualityScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QualityScoreRepository extends JpaRepository<QualityScore, UUID> {

    Page<QualityScore> findByProjectIdOrderByCalculatedAtDesc(UUID projectId, Pageable pageable);

    Optional<QualityScore> findFirstByProjectIdOrderByCalculatedAtDesc(UUID projectId);

    java.util.List<QualityScore> findByDeletedFalse();

    long countByDeletedFalse();

    @org.springframework.data.jpa.repository.Query("SELECT AVG(qs.overallScore) FROM QualityScore qs WHERE qs.deleted = false")
    java.math.BigDecimal findAverageOverallScore();
}
