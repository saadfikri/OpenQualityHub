package com.openqualityhub.repository;

import com.openqualityhub.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByNameAndOrganizationId(String name, UUID organizationId);

    Optional<Role> findByIdAndDeletedFalse(UUID id);

    List<Role> findByOrganizationId(UUID organizationId);
}
