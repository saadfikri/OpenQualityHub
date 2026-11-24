package com.openqualityhub.repository;

import com.openqualityhub.entity.SlaEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SlaEvaluationRepository extends JpaRepository<SlaEvaluation, UUID> {

    List<SlaEvaluation> findByRequestIdOrderByEvaluatedAtDesc(UUID requestId);

    @Query("SELECT COUNT(e) FROM SlaEvaluation e WHERE e.responseMet = false")
    long countResponseBreaches();

    @Query("SELECT COUNT(e) FROM SlaEvaluation e WHERE e.resolutionMet = false")
    long countResolutionBreaches();
}
