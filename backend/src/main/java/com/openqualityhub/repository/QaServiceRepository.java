package com.openqualityhub.repository;

import com.openqualityhub.entity.QaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QaServiceRepository extends JpaRepository<QaService, UUID> {

    Page<QaService> findByDeletedFalseAndActiveTrue(Pageable pageable);

    Page<QaService> findByDeletedFalse(Pageable pageable);

    Page<QaService> findByCategoryAndDeletedFalseAndActiveTrue(QaService.ServiceCategory category, Pageable pageable);

    Optional<QaService> findByIdAndDeletedFalse(UUID id);

    List<QaService> findByDeletedFalseAndActiveTrue();

    long countByDeletedFalseAndActiveTrue();
}
