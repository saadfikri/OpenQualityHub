package com.openqualityhub.repository;

import com.openqualityhub.entity.CapacityAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CapacityAllocationRepository extends JpaRepository<CapacityAllocation, UUID> {

    List<CapacityAllocation> findByTeamIdAndWeekStartBetweenAndDeletedFalse(UUID teamId, LocalDate from, LocalDate to);

    List<CapacityAllocation> findByUserIdAndWeekStartBetweenAndDeletedFalse(UUID userId, LocalDate from, LocalDate to);

    Optional<CapacityAllocation> findByTeamIdAndUserIsNullAndWeekStartAndDeletedFalse(UUID teamId, LocalDate weekStart);

    Optional<CapacityAllocation> findByUserIdAndWeekStartAndDeletedFalse(UUID userId, LocalDate weekStart);

    @Query("SELECT ca FROM CapacityAllocation ca WHERE ca.weekStart = :weekStart AND ca.deleted = false ORDER BY ca.team.name")
    List<CapacityAllocation> findAllByWeekStart(@Param("weekStart") LocalDate weekStart);

    @Query("SELECT ca FROM CapacityAllocation ca WHERE ca.team.id = :teamId AND ca.deleted = false " +
           "AND ca.weekStart >= :from ORDER BY ca.weekStart ASC")
    List<CapacityAllocation> findUpcomingByTeam(@Param("teamId") UUID teamId, @Param("from") LocalDate from);
}
