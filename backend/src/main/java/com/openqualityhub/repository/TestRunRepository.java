package com.openqualityhub.repository;

import com.openqualityhub.entity.TestRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestRunRepository extends JpaRepository<TestRun, UUID> {

    @EntityGraph(attributePaths = {"project"})
    Optional<TestRun> findByIdAndDeletedFalse(UUID id);

    @EntityGraph(attributePaths = {"project"})
    Page<TestRun> findByDeletedFalse(Pageable pageable);

    List<TestRun> findByDeletedFalse();

    List<TestRun> findByProjectIdAndDeletedFalse(UUID projectId);

    Page<TestRun> findByProjectId(UUID projectId, Pageable pageable);

    @EntityGraph(attributePaths = {"project"})
    Page<TestRun> findByProjectIdAndDeletedFalseOrderByExecutedAtDesc(UUID projectId, Pageable pageable);

    Page<TestRun> findByProjectIdOrderByExecutedAtDesc(UUID projectId, Pageable pageable);

    @Query("SELECT AVG(tr.passRate) FROM TestRun tr WHERE tr.deleted = false")
    java.math.BigDecimal findAveragePassRate();

    @Query(value = "SELECT DATE(executed_at) as day, AVG(pass_rate), SUM(total_tests), SUM(passed), SUM(failed) " +
           "FROM test_runs WHERE executed_at >= :since AND is_deleted = false " +
           "GROUP BY DATE(executed_at) ORDER BY day", nativeQuery = true)
    List<Object[]> findDailyPassRateTrend(Instant since);

    @EntityGraph(attributePaths = {"project"})
    Page<TestRun> findByDeletedFalseOrderByExecutedAtDesc(Pageable pageable);

    long countByProjectIdAndDeletedFalse(UUID projectId);

    @Query("SELECT AVG(tr.passRate) FROM TestRun tr WHERE tr.project.id = :projectId AND tr.deleted = false")
    java.math.BigDecimal findAveragePassRateByProjectId(UUID projectId);

    @Query("SELECT AVG(tr.passRate) FROM TestRun tr WHERE tr.release.id = :releaseId AND tr.deleted = false")
    java.math.BigDecimal findAveragePassRateByReleaseId(UUID releaseId);

    long countByReleaseIdAndDeletedFalse(UUID releaseId);

    long countByDeletedFalse();

    @Query(value = "SELECT DATE(executed_at) as day, AVG(pass_rate) as avg_pass_rate, SUM(total_tests) as total_tests " +
           "FROM test_runs WHERE executed_at >= :since AND is_deleted = false AND project_id = :projectId " +
           "GROUP BY DATE(executed_at) ORDER BY day", nativeQuery = true)
    List<Object[]> findDailyPassRateTrendByProject(Instant since, UUID projectId);

    @Query(value = "SELECT DATE(executed_at) as day, COUNT(*) as runs, SUM(total_tests) as tests " +
           "FROM test_runs WHERE executed_at >= :since AND is_deleted = false " +
           "GROUP BY DATE(executed_at) ORDER BY day", nativeQuery = true)
    List<Object[]> findDailyTestVolume(Instant since);

    @Query(value = "SELECT DATE(executed_at) as day, COUNT(*) as runs, SUM(total_tests) as tests " +
           "FROM test_runs WHERE executed_at >= :since AND is_deleted = false AND project_id = :projectId " +
           "GROUP BY DATE(executed_at) ORDER BY day", nativeQuery = true)
    List<Object[]> findDailyTestVolumeByProject(Instant since, UUID projectId);
}
