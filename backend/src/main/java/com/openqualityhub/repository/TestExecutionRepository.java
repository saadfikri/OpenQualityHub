package com.openqualityhub.repository;

import com.openqualityhub.entity.TestExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface TestExecutionRepository extends JpaRepository<TestExecution, UUID> {

    Page<TestExecution> findByTestIdentityIdOrderByExecutedAtDesc(UUID testIdentityId, Pageable pageable);

    List<TestExecution> findByTestIdentityIdAndExecutedAtAfterOrderByExecutedAtDesc(
        UUID testIdentityId, Instant since);

    long countByTestRunId(UUID testRunId);

    @Query("SELECT te.status, COUNT(te) FROM TestExecution te " +
           "WHERE te.testIdentity.id = :testIdentityId AND te.executedAt >= :since " +
           "GROUP BY te.status")
    List<Object[]> countByStatusSince(UUID testIdentityId, Instant since);
}
