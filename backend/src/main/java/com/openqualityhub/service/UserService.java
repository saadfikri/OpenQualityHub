package com.openqualityhub.service;

import com.openqualityhub.dto.request.AssignRoleRequest;
import com.openqualityhub.dto.request.ChangePasswordRequest;
import com.openqualityhub.dto.request.UpdateUserRequest;
import com.openqualityhub.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {
    Page<UserResponse> getAll(Pageable pageable);
    UserResponse getById(UUID id);
    UserResponse getMe(UUID userId);
    UserResponse update(UUID id, UpdateUserRequest request);
    void changePassword(UUID userId, ChangePasswordRequest request);
    UserResponse assignRole(UUID userId, AssignRoleRequest request);
    UserResponse deactivate(UUID id);
    UserResponse activate(UUID id);
}
