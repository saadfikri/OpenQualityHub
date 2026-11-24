package com.openqualityhub.repository;

import com.openqualityhub.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    Page<Application> findByProjectId(UUID projectId, Pageable pageable);

    boolean existsByNameAndProjectId(String name, UUID projectId);
}
