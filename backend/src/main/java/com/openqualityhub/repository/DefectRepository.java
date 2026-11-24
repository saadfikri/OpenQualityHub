package com.openqualityhub.repository;

import com.openqualityhub.entity.Defect;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DefectRepository extends JpaRepository<Defect, UUID> {

    Optional<Defect> findByIdAndDeletedFalse(UUID id);

    Page<Defect> findByDeletedFalse(Pageable pageable);

    List<Defect> findAllByDeletedFalse();

    List<Defect> findAllByProjectIdAndDeletedFalse(UUID projectId);

    List<Defect> findAllByStatusAndDeletedFalse(Defect.DefectStatus status);

    List<Defect> findAllByProjectIdAndStatusAndDeletedFalse(UUID projectId, Defect.DefectStatus status);

    Page<Defect> findBySeverityAndDeletedFalse(Defect.Severity severity, Pageable pageable);

    Page<Defect> findByProjectIdAndDeletedFalse(UUID projectId, Pageable pageable);

    Page<Defect> findByProjectIdAndSeverityAndDeletedFalse(UUID projectId, Defect.Severity severity, Pageable pageable);

    Page<Defect> findByProjectIdAndStatusAndDeletedFalse(UUID projectId, Defect.DefectStatus status, Pageable pageable);

    long countByProjectIdAndSeverityAndStatusNotAndDeletedFalse(UUID projectId, Defect.Severity severity, Defect.DefectStatus excludeStatus);

    @Query("SELECT COUNT(d) FROM Defect d WHERE d.project.id = :projectId AND d.status NOT IN ('CLOSED', 'RESOLVED') AND d.deleted = false")
    long countOpenByProjectId(UUID projectId);

    Page<Defect> findByStatusAndDeletedFalse(Defect.DefectStatus status, Pageable pageable);

    long countByStatusAndDeletedFalse(Defect.DefectStatus status);

    long countBySeverityAndDeletedFalse(Defect.Severity severity);

    long countByReleaseIdAndSeverityAndStatusNotAndDeletedFalse(UUID releaseId, Defect.Severity severity, Defect.DefectStatus excludedStatus);

    @Query("SELECT COUNT(d) FROM Defect d WHERE d.release.id = :releaseId AND d.status NOT IN ('CLOSED', 'RESOLVED') AND d.deleted = false")
    long countOpenByReleaseId(UUID releaseId);

    long countByStatusNotInAndDeletedFalse(java.util.List<Defect.DefectStatus> statuses);

    long countByDeletedFalse();

    @Query(value = "SELECT DATE(created_at) as day, COUNT(*) as cnt " +
           "FROM defects WHERE created_at >= :since AND is_deleted = false " +
           "GROUP BY DATE(created_at) ORDER BY day", nativeQuery = true)
    List<Object[]> findDailyOpenedSince(java.time.Instant since);

    @Query(value = "SELECT DATE(updated_at) as day, COUNT(*) as cnt " +
           "FROM defects WHERE updated_at >= :since AND is_deleted = false AND status IN ('RESOLVED', 'CLOSED') " +
           "GROUP BY DATE(updated_at) ORDER BY day", nativeQuery = true)
    List<Object[]> findDailyResolvedSince(java.time.Instant since);
}
