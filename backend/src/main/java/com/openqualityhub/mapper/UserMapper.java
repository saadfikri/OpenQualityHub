package com.openqualityhub.mapper;

import com.openqualityhub.dto.response.UserResponse;
import com.openqualityhub.entity.Role;
import com.openqualityhub.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.Set;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(source = "roles", target = "roleName", qualifiedByName = "roleName")
    @Mapping(source = "roles", target = "roleId", qualifiedByName = "roleId")
    UserResponse toResponse(User user);

    @Named("roleName")
    default String mapRoleName(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) return null;
        return roles.iterator().next().getName();
    }

    @Named("roleId")
    default UUID mapRoleId(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) return null;
        return roles.iterator().next().getId();
    }
}
