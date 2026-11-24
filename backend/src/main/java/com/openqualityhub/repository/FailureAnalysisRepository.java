package com.openqualityhub.repository;

import com.openqualityhub.entity.FailureAnalysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FailureAnalysisRepository extends JpaRepository<FailureAnalysis, UUID> {

    Optional<FailureAnalysis> findByTestRunIdAndDeletedFalse(UUID testRunId);

    Optional<FailureAnalysis> findByIdAndDeletedFalse(UUID id);

    Page<FailureAnalysis> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    long countByHumanFeedbackAndDeletedFalse(FailureAnalysis.HumanFeedback feedback);

    long countByFailureCategoryAndDeletedFalse(FailureAnalysis.FailureCategory category);

    long countByDeletedFalse();
}
