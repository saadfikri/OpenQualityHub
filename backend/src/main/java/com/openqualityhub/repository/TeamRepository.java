package com.openqualityhub.repository;

import com.openqualityhub.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    boolean existsByNameAndDeletedFalse(String name);

    Optional<Team> findByIdAndDeletedFalse(UUID id);

    Page<Team> findByDeletedFalse(Pageable pageable);

    java.util.List<Team> findByDeletedFalse();

    Page<Team> findByDeletedFalseAndNameContainingIgnoreCase(String name, Pageable pageable);

    long countByDeletedFalse();

    @Query("SELECT t FROM Team t LEFT JOIN FETCH t.projects")
    Page<Team> findAllWithProjects(Pageable pageable);

    Page<Team> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
