package com.openqualityhub.repository;

import com.openqualityhub.entity.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EnvironmentRepository extends JpaRepository<Environment, UUID> {

    Page<Environment> findByOrganizationIdAndDeletedFalse(UUID organizationId, Pageable pageable);
}
