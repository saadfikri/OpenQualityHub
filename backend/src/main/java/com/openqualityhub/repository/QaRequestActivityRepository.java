package com.openqualityhub.repository;

import com.openqualityhub.entity.QaRequestActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QaRequestActivityRepository extends JpaRepository<QaRequestActivity, UUID> {

    List<QaRequestActivity> findByRequestIdOrderByCreatedAtAsc(UUID requestId);

    Page<QaRequestActivity> findByRequestId(UUID requestId, Pageable pageable);
}
