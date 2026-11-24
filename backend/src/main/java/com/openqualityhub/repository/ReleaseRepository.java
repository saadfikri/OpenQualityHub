package com.openqualityhub.repository;

import com.openqualityhub.entity.Release;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReleaseRepository extends JpaRepository<Release, UUID> {

    Optional<Release> findByIdAndDeletedFalse(UUID id);

    Page<Release> findByDeletedFalse(Pageable pageable);

    Page<Release> findByProjectIdAndDeletedFalse(UUID projectId, Pageable pageable);

    boolean existsByReleaseVersionAndProjectId(String releaseVersion, UUID projectId);

    java.util.List<Release> findByStatusInAndDeletedFalse(java.util.List<Release.ReleaseStatus> statuses);
}
