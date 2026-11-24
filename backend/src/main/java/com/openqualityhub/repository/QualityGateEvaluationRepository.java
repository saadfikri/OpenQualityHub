package com.openqualityhub.repository;

import com.openqualityhub.entity.QualityGateEvaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QualityGateEvaluationRepository extends JpaRepository<QualityGateEvaluation, UUID> {

    Page<QualityGateEvaluation> findByProjectIdOrderByEvaluatedAtDesc(UUID projectId, Pageable pageable);

    Optional<QualityGateEvaluation> findFirstByProjectIdAndReleaseIdOrderByEvaluatedAtDesc(UUID projectId, UUID releaseId);
}
