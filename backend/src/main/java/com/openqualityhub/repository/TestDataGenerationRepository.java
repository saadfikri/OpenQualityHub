package com.openqualityhub.repository;

import com.openqualityhub.entity.TestDataGeneration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestDataGenerationRepository extends JpaRepository<TestDataGeneration, UUID> {

    Page<TestDataGeneration> findByTemplateIdAndDeletedFalseOrderByCreatedAtDesc(UUID templateId, Pageable pageable);

    Optional<TestDataGeneration> findByIdAndDeletedFalse(UUID id);

    Page<TestDataGeneration> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    long countByDeletedFalse();
}
