package com.openqualityhub.repository;

import com.openqualityhub.entity.ContributorActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContributorActivityRepository extends JpaRepository<ContributorActivity, UUID> {

    Page<ContributorActivity> findByContributorId(UUID contributorId, Pageable pageable);

    @Query("SELECT ca FROM ContributorActivity ca WHERE ca.project.id = :projectId")
    Page<ContributorActivity> findByProjectId(UUID projectId, Pageable pageable);

    @Query("SELECT ca.activityType, COUNT(ca) FROM ContributorActivity ca WHERE ca.contributor.id = :contributorId GROUP BY ca.activityType")
    List<Object[]> countByContributorGroupedByType(UUID contributorId);

    @Query("SELECT DISTINCT ca.project.id, ca.project.name FROM ContributorActivity ca WHERE ca.contributor.id = :contributorId AND ca.project IS NOT NULL")
    List<Object[]> findDistinctProjectsByContributor(UUID contributorId);
}
