package com.openqualityhub.repository;

import com.openqualityhub.entity.TestCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {

    Page<TestCase> findByTestRunId(UUID testRunId, Pageable pageable);

    Page<TestCase> findByTestRunIdAndStatus(UUID testRunId, TestCase.TestStatus status, Pageable pageable);

    long countByDeletedFalse();
}
