package com.openqualityhub.repository;

import com.openqualityhub.entity.SlaConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SlaConfigurationRepository extends JpaRepository<SlaConfiguration, UUID> {

    Page<SlaConfiguration> findByDeletedFalse(Pageable pageable);

    Optional<SlaConfiguration> findByIdAndDeletedFalse(UUID id);

    Optional<SlaConfiguration> findByPriorityLevelAndOrganizationIdAndDeletedFalse(int priorityLevel, UUID organizationId);

    List<SlaConfiguration> findByDeletedFalseAndActiveTrueOrderByPriorityLevelAsc();
}
