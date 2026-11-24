package com.openqualityhub.repository;

import com.openqualityhub.entity.TestIdentity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestIdentityRepository extends JpaRepository<TestIdentity, UUID> {

    Optional<TestIdentity> findByProjectIdAndFingerprint(UUID projectId, String fingerprint);

    Page<TestIdentity> findByProjectIdOrderByFlakyScoreDesc(UUID projectId, Pageable pageable);

    Page<TestIdentity> findByProjectIdAndFlakyTrueOrderByFlakyScoreDesc(UUID projectId, Pageable pageable);

    long countByProjectIdAndFlakyTrue(UUID projectId);

    @Query("SELECT COUNT(ti) FROM TestIdentity ti WHERE ti.flaky = true")
    long countAllFlaky();

    @Query("SELECT ti FROM TestIdentity ti WHERE ti.project.id = :projectId AND ti.totalExecutions >= :minExecutions ORDER BY ti.flakyScore DESC")
    Page<TestIdentity> findFlakyCandidate(UUID projectId, int minExecutions, Pageable pageable);

    Page<TestIdentity> findByFlakyTrueOrderByFlakyScoreDesc(Pageable pageable);

    long countByProjectId(UUID projectId);

    long countByFailureClassification(TestIdentity.FailureClassification classification);

    @Query(value = "SELECT DATE(last_seen_at) as day, COUNT(*) as cnt " +
           "FROM test_identities WHERE is_flaky = true AND last_seen_at >= :since " +
           "GROUP BY DATE(last_seen_at) ORDER BY day", nativeQuery = true)
    List<Object[]> findDailyFlakyCount(java.time.Instant since);

    @Query(value = "SELECT DATE(last_seen_at) as day, COUNT(*) as cnt " +
           "FROM test_identities WHERE is_flaky = true AND last_seen_at >= :since AND project_id = :projectId " +
           "GROUP BY DATE(last_seen_at) ORDER BY day", nativeQuery = true)
    List<Object[]> findDailyFlakyCountByProject(java.time.Instant since, java.util.UUID projectId);
}
