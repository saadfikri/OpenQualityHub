package com.openqualityhub.repository;

import com.openqualityhub.entity.ReleaseReadinessReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReleaseReadinessReportRepository extends JpaRepository<ReleaseReadinessReport, UUID> {

    Optional<ReleaseReadinessReport> findFirstByReleaseIdAndDeletedFalseOrderByGeneratedAtDesc(UUID releaseId);

    Page<ReleaseReadinessReport> findByReleaseIdAndDeletedFalseOrderByGeneratedAtDesc(UUID releaseId, Pageable pageable);
}
