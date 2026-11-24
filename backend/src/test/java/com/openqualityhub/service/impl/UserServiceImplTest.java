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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private User user;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .email("test@demo.oqh")
                .firstName("John")
                .lastName("Doe")
                .displayName("John Doe")
                .passwordHash("encodedPassword")
                .roles(new HashSet<>())
                .build();
        user.setId(userId);

        userResponse = UserResponse.builder()
                .id(userId)
                .email("test@demo.oqh")
                .firstName("John")
                .lastName("Doe")
                .displayName("John Doe")
                .build();
    }

    // --- getAll ---

    @Test
    void getAll_returnsPageOfUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);
        when(userRepository.findByDeletedFalse(pageable)).thenReturn(userPage);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        Page<UserResponse> result = userService.getAll(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("test@demo.oqh", result.getContent().get(0).getEmail());
    }

    // --- getById ---

    @Test
    void getById_found_returnsResponse() {
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.getById(userId);

        assertEquals(userId, result.getId());
    }

    @Test
    void getById_notFound_throwsException() {
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getById(userId));
    }

    // --- getMe ---

    @Test
    void getMe_found_returnsResponse() {
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.getMe(userId);

        assertEquals(userId, result.getId());
    }

    @Test
    void getMe_notFound_throwsException() {
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getMe(userId));
    }

    // --- update ---

    @Test
    void update_allFields_updatesAllFields() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .displayName("Jane Smith")
                .build();

        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        userService.update(userId, request);

        assertEquals("Jane", user.getFirstName());
        assertEquals("Smith", user.getLastName());
        assertEquals("Jane Smith", user.getDisplayName());
        verify(userRepository).save(user);
    }

    @Test
    void update_nullFields_doesNotOverwrite() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName(null)
                .lastName(null)
                .displayName(null)
                .build();

        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        userService.update(userId, request);

        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("John Doe", user.getDisplayName());
    }

    @Test
    void update_partialFields_updatesOnlyProvided() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("Updated")
                .build();

        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        userService.update(userId, request);

        assertEquals("Updated", user.getFirstName());
        assertEquals("Doe", user.getLastName()); // unchanged
    }

    @Test
    void update_notFound_throwsException() {
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.update(userId, new UpdateUserRequest()));
    }

    // --- changePassword ---

    @Test
    void changePassword_correctOldPassword_encodesNewPassword() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("oldPass")
                .newPassword("newPass123")
                .build();

        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPass123")).thenReturn("newEncodedPassword");

        userService.changePassword(userId, request);

        assertEquals("newEncodedPassword", user.getPasswordHash());
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_incorrectOldPassword_throwsIllegalArgument() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("wrongPass")
                .newPassword("newPass123")
                .build();

        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "encodedPassword")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> userService.changePassword(userId, request));

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_userNotFound_throwsException() {
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.changePassword(userId, new ChangePasswordRequest()));
    }

    // --- assignRole ---

    @Test
    void assignRole_clearsOldRolesAndAddsNew() {
        UUID roleId = UUID.randomUUID();
        AssignRoleRequest request = AssignRoleRequest.builder().roleId(roleId).build();

        Role oldRole = Role.builder().name("OLD_ROLE").build();
        oldRole.setId(UUID.randomUUID());
        user.getRoles().add(oldRole);

        Role newRole = Role.builder().name("QA_ENGINEER").build();
        newRole.setId(roleId);

        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByIdAndDeletedFalse(roleId)).thenReturn(Optional.of(newRole));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        userService.assignRole(userId, request);

        assertEquals(1, user.getRoles().size());
        assertTrue(user.getRoles().contains(newRole));
        verify(userRepository).save(user);
    }

    @Test
    void assignRole_userNotFound_throwsException() {
        UUID roleId = UUID.randomUUID();
        AssignRoleRequest request = AssignRoleRequest.builder().roleId(roleId).build();

        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.assignRole(userId, request));
    }

    @Test
    void assignRole_roleNotFound_throwsException() {
        UUID roleId = UUID.randomUUID();
        AssignRoleRequest request = AssignRoleRequest.builder().roleId(roleId).build();

        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByIdAndDeletedFalse(roleId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.assignRole(userId, request));
    }

    // --- deactivate ---

    @Test
    void deactivate_setsActiveFalse() {
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        userService.deactivate(userId);

        assertFalse(user.isActive());
        verify(userRepository).save(user);
    }

    @Test
    void deactivate_notFound_throwsException() {
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.deactivate(userId));
    }

    // --- activate ---

    @Test
    void activate_setsActiveTrue() {
        user.setActive(false);

        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        userService.activate(userId);

        assertTrue(user.isActive());
        verify(userRepository).save(user);
    }

    @Test
    void activate_notFound_throwsException() {
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.activate(userId));
    }
}
