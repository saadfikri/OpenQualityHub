package com.openqualityhub.repository;

import com.openqualityhub.entity.ReleaseSignOff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReleaseSignOffRepository extends JpaRepository<ReleaseSignOff, UUID> {

    List<ReleaseSignOff> findByReleaseIdAndDeletedFalse(UUID releaseId);

    Optional<ReleaseSignOff> findByReleaseIdAndUserIdAndDeletedFalse(UUID releaseId, UUID userId);

    long countByReleaseIdAndDecisionAndDeletedFalse(UUID releaseId, ReleaseSignOff.Decision decision);
}
