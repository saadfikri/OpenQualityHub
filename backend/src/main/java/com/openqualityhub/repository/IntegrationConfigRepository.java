package com.openqualityhub.repository;

import com.openqualityhub.entity.IntegrationConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntegrationConfigRepository extends JpaRepository<IntegrationConfig, UUID> {

    Optional<IntegrationConfig> findByIdAndDeletedFalse(UUID id);

    Page<IntegrationConfig> findByDeletedFalse(Pageable pageable);

    Page<IntegrationConfig> findByProviderAndDeletedFalse(IntegrationConfig.Provider provider, Pageable pageable);

    Page<IntegrationConfig> findByProjectIdAndDeletedFalse(UUID projectId, Pageable pageable);

    List<IntegrationConfig> findByEnabledTrueAndDeletedFalse();

    long countByDeletedFalse();

    long countByProviderAndDeletedFalse(IntegrationConfig.Provider provider);

    long countByEnabledTrueAndDeletedFalse();

    boolean existsByNameAndOrganizationIdAndDeletedFalse(String name, UUID organizationId);
}
