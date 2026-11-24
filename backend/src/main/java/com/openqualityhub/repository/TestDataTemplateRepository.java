package com.openqualityhub.repository;

import com.openqualityhub.entity.TestDataTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestDataTemplateRepository extends JpaRepository<TestDataTemplate, UUID> {

    @EntityGraph(attributePaths = {"project", "fields", "profiles", "generations"})
    Optional<TestDataTemplate> findByIdAndDeletedFalse(UUID id);

    @EntityGraph(attributePaths = {"project", "fields", "profiles", "generations"})
    Page<TestDataTemplate> findByDeletedFalse(Pageable pageable);

    @EntityGraph(attributePaths = {"project", "fields", "profiles", "generations"})
    Page<TestDataTemplate> findByProjectIdAndDeletedFalse(UUID projectId, Pageable pageable);

    long countByDeletedFalse();

    boolean existsByNameAndOrganizationIdAndDeletedFalse(String name, UUID organizationId);
}
