package com.openqualityhub.repository;

import com.openqualityhub.entity.QualityGate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QualityGateRepository extends JpaRepository<QualityGate, UUID> {

    Page<QualityGate> findByOrganizationIdAndDeletedFalse(UUID organizationId, Pageable pageable);

    Optional<QualityGate> findByOrganizationIdAndDefaultGateTrue(UUID organizationId);
}
