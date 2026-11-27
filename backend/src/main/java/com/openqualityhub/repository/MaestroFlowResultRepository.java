package com.openqualityhub.repository;

import com.openqualityhub.entity.MaestroFlowResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MaestroFlowResultRepository extends JpaRepository<MaestroFlowResult, UUID> {

    List<MaestroFlowResult> findByTestRunIdAndDeletedFalse(UUID testRunId);

    Page<MaestroFlowResult> findByDeletedFalse(Pageable pageable);

    Page<MaestroFlowResult> findByDevicePlatformAndDeletedFalse(MaestroFlowResult.DevicePlatform platform, Pageable pageable);

    @Query(value = "SELECT device_name, os_version, device_platform, status, COUNT(*) " +
           "FROM maestro_flow_results WHERE is_deleted = false " +
           "GROUP BY device_name, os_version, device_platform, status", nativeQuery = true)
    List<Object[]> getDeviceMatrix();

    @Query(value = "SELECT flow_name, status, COUNT(*), AVG(duration_ms) " +
           "FROM maestro_flow_results WHERE is_deleted = false " +
           "GROUP BY flow_name, status", nativeQuery = true)
    List<Object[]> getFlowAnalytics();

    @Query(value = "SELECT device_platform, COUNT(*), " +
           "SUM(CASE WHEN status = 'PASSED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) " +
           "FROM maestro_flow_results WHERE is_deleted = false " +
           "GROUP BY device_platform", nativeQuery = true)
    List<Object[]> getPlatformSummary();

    @Query(value = "SELECT app_id, app_version, COUNT(*), " +
           "SUM(CASE WHEN status = 'PASSED' THEN 1 ELSE 0 END) " +
           "FROM maestro_flow_results WHERE is_deleted = false AND app_id IS NOT NULL " +
           "GROUP BY app_id, app_version ORDER BY app_version DESC", nativeQuery = true)
    List<Object[]> getAppVersionSummary();

    long countByDeletedFalse();

    long countByStatusAndDeletedFalse(MaestroFlowResult.FlowStatus status);

    @Query(value = "SELECT flow_name, COUNT(*) FROM maestro_flow_results " +
           "WHERE is_deleted = false AND status = 'FAILED' " +
           "GROUP BY flow_name ORDER BY COUNT(*) DESC LIMIT 10", nativeQuery = true)
    List<Object[]> getTopFailingFlows();

    List<MaestroFlowResult> findByFlowNameAndDeletedFalse(String flowName);
}
