package com.openqualityhub.repository;

import com.openqualityhub.entity.QaServiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QaServiceRequestRepository extends JpaRepository<QaServiceRequest, UUID> {

    @EntityGraph(attributePaths = {"qaService", "requestingTeam", "requestingUser", "project", "assignedTeam", "assignedUser"})
    Optional<QaServiceRequest> findByIdAndDeletedFalse(UUID id);

    @EntityGraph(attributePaths = {"qaService", "requestingTeam", "requestingUser", "project", "assignedTeam", "assignedUser"})
    Page<QaServiceRequest> findByDeletedFalse(Pageable pageable);

    @EntityGraph(attributePaths = {"qaService", "requestingTeam", "requestingUser", "project", "assignedTeam", "assignedUser"})
    Page<QaServiceRequest> findByStatusAndDeletedFalse(QaServiceRequest.RequestStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"qaService", "requestingTeam", "requestingUser", "project", "assignedTeam", "assignedUser"})
    Page<QaServiceRequest> findByRequestingTeamIdAndDeletedFalse(UUID teamId, Pageable pageable);

    @EntityGraph(attributePaths = {"qaService", "requestingTeam", "requestingUser", "project", "assignedTeam", "assignedUser"})
    Page<QaServiceRequest> findByAssignedUserIdAndDeletedFalse(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"qaService", "requestingTeam", "requestingUser", "project", "assignedTeam", "assignedUser"})
    Page<QaServiceRequest> findByAssignedTeamIdAndDeletedFalse(UUID teamId, Pageable pageable);

    long countByStatusAndDeletedFalse(QaServiceRequest.RequestStatus status);

    long countByAssignedUserIdAndStatusInAndDeletedFalse(UUID userId, java.util.Collection<QaServiceRequest.RequestStatus> statuses);

    long countByAssignedTeamIdAndStatusInAndDeletedFalse(UUID teamId, java.util.Collection<QaServiceRequest.RequestStatus> statuses);

    @Query("SELECT r FROM QaServiceRequest r WHERE r.deleted = false AND r.status NOT IN ('CLOSED', 'CANCELLED') " +
           "AND r.resolutionDeadline IS NOT NULL AND r.resolutionDeadline < CURRENT_TIMESTAMP")
    Page<QaServiceRequest> findBreachedRequests(Pageable pageable);

    @Query(value = "SELECT nextval('qa_request_number_seq')", nativeQuery = true)
    long getNextRequestNumber();

    @Query("SELECT COUNT(r) FROM QaServiceRequest r WHERE r.assignedUser.id = :userId AND r.deleted = false " +
           "AND r.status IN ('ASSIGNED', 'IN_PROGRESS', 'ON_HOLD')")
    long countActiveByAssignedUser(@Param("userId") UUID userId);

    @Query("SELECT COUNT(r) FROM QaServiceRequest r WHERE r.assignedTeam.id = :teamId AND r.deleted = false " +
           "AND r.status IN ('ASSIGNED', 'IN_PROGRESS', 'ON_HOLD')")
    long countActiveByAssignedTeam(@Param("teamId") UUID teamId);

    @Query("SELECT COALESCE(SUM(r.estimatedHours), 0) FROM QaServiceRequest r WHERE r.assignedUser.id = :userId " +
           "AND r.deleted = false AND r.status IN ('ASSIGNED', 'IN_PROGRESS', 'ON_HOLD')")
    java.math.BigDecimal sumEstimatedHoursByAssignedUser(@Param("userId") UUID userId);

    @Query("SELECT COALESCE(SUM(r.estimatedHours), 0) FROM QaServiceRequest r WHERE r.assignedTeam.id = :teamId " +
           "AND r.deleted = false AND r.status IN ('ASSIGNED', 'IN_PROGRESS', 'ON_HOLD')")
    java.math.BigDecimal sumEstimatedHoursByAssignedTeam(@Param("teamId") UUID teamId);
}
