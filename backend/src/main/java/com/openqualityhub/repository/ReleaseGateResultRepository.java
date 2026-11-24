package com.openqualityhub.repository;

import com.openqualityhub.entity.ReleaseGateResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReleaseGateResultRepository extends JpaRepository<ReleaseGateResult, UUID> {

    List<ReleaseGateResult> findByReportId(UUID reportId);
}
