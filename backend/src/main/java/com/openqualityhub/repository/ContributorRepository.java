package com.openqualityhub.repository;

import com.openqualityhub.entity.Contributor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContributorRepository extends JpaRepository<Contributor, UUID> {

    @Query("SELECT c FROM Contributor c WHERE c.organization.id = :orgId AND c.deleted = false")
    Page<Contributor> findByOrganizationId(UUID orgId, Pageable pageable);

    @Query("SELECT c FROM Contributor c WHERE c.organization.id = :orgId AND c.provider = :provider AND c.externalId = :externalId AND c.deleted = false")
    Optional<Contributor> findByOrgAndProviderAndExternalId(UUID orgId, Contributor.Provider provider, String externalId);

    @Query("SELECT c FROM Contributor c WHERE c.organization.id = :orgId AND LOWER(c.username) LIKE LOWER(CONCAT('%', :search, '%')) AND c.deleted = false")
    Page<Contributor> searchByUsername(UUID orgId, String search, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Contributor c WHERE c.organization.id = :orgId AND c.deleted = false")
    long countByOrganizationId(UUID orgId);
}
