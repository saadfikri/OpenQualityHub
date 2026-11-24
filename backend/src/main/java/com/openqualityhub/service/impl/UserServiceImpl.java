package com.openqualityhub.service.impl;

import com.openqualityhub.dto.request.AssignRoleRequest;
import com.openqualityhub.dto.request.ChangePasswordRequest;
import com.openqualityhub.dto.request.UpdateUserRequest;
import com.openqualityhub.dto.response.UserResponse;
import com.openqualityhub.entity.Role;
import com.openqualityhub.entity.User;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.mapper.UserMapper;
import com.openqualityhub.repository.RoleRepository;
import com.openqualityhub.repository.UserRepository;
import com.openqualityhub.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    public Page<UserResponse> getAll(Pageable pageable) {
        return userRepository.findByDeletedFalse(pageable).map(userMapper::toResponse);
    }

    @Override
    public UserResponse getById(UUID id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse getMe(UUID userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = userRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getDisplayName() != null) user.setDisplayName(request.getDisplayName());

        user = userRepository.save(user);
        log.info("Updated user: {}", id);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", userId);
    }

    @Override
    @Transactional
    public UserResponse assignRole(UUID userId, AssignRoleRequest request) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Role role = roleRepository.findByIdAndDeletedFalse(request.getRoleId())
            .orElseThrow(() -> new ResourceNotFoundException("Role", request.getRoleId()));

        user.getRoles().clear();
        user.getRoles().add(role);
        user = userRepository.save(user);
        log.info("Assigned role '{}' to user: {}", role.getName(), userId);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse deactivate(UUID id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));

        user.setActive(false);
        user = userRepository.save(user);
        log.info("Deactivated user: {}", id);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse activate(UUID id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));

        user.setActive(true);
        user = userRepository.save(user);
        log.info("Activated user: {}", id);
        return userMapper.toResponse(user);
    }
}
