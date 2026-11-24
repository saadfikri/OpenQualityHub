package com.openqualityhub.repository;

import com.openqualityhub.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByIdAndDeletedFalse(UUID id);

    Page<Project> findByDeletedFalse(Pageable pageable);

    List<Project> findByDeletedFalse();

    Page<Project> findByTeamIdAndDeletedFalse(UUID teamId, Pageable pageable);

    Page<Project> findByStatusAndDeletedFalse(Project.ProjectStatus status, Pageable pageable);

    boolean existsByNameAndTeamIdAndDeletedFalse(String name, UUID teamId);

    long countByDeletedFalse();

    Page<Project> findByTeamId(UUID teamId, Pageable pageable);

    Page<Project> findByStatus(Project.ProjectStatus status, Pageable pageable);

    boolean existsByNameAndTeamId(String name, UUID teamId);
}
