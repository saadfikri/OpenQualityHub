package com.openqualityhub.repository;

import com.openqualityhub.entity.Build;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BuildRepository extends JpaRepository<Build, UUID> {

    Page<Build> findByProjectIdAndDeletedFalse(UUID projectId, Pageable pageable);

    Page<Build> findByReleaseId(UUID releaseId, Pageable pageable);

    long countByReleaseIdAndStatusAndDeletedFalse(UUID releaseId, Build.BuildStatus status);

    long countByReleaseIdAndDeletedFalse(UUID releaseId);
}
