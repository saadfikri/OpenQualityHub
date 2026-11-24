package com.openqualityhub.repository;

import com.openqualityhub.entity.TestDataProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestDataProfileRepository extends JpaRepository<TestDataProfile, UUID> {

    List<TestDataProfile> findByTemplateIdAndDeletedFalse(UUID templateId);

    Optional<TestDataProfile> findByIdAndDeletedFalse(UUID id);

    boolean existsByNameAndTemplateIdAndDeletedFalse(String name, UUID templateId);

    long countByDeletedFalse();
}
