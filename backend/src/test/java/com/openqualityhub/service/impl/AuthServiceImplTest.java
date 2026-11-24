package com.openqualityhub.service.impl;

import com.openqualityhub.dto.request.LoginRequest;
import com.openqualityhub.dto.request.RegisterRequest;
import com.openqualityhub.dto.response.AuthResponse;
import com.openqualityhub.entity.Organization;
import com.openqualityhub.entity.Permission;
import com.openqualityhub.entity.Role;
import com.openqualityhub.entity.User;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.repository.OrganizationRepository;
import com.openqualityhub.repository.RoleRepository;
import com.openqualityhub.repository.UserRepository;
import com.openqualityhub.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private static final UUID DEFAULT_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private Organization org;
    private Role role;
    private User user;

    @BeforeEach
    void setUp() {
        org = Organization.builder().name("Demo Org").build();
        org.setId(DEFAULT_ORG_ID);

        Permission perm = Permission.builder().name("TEST_READ").build();
        role = Role.builder().name("QA_ENGINEER").organization(org).permissions(Set.of(perm)).build();

        UUID userId = UUID.randomUUID();
        user = User.builder()
            .organization(org)
            .email("test@example.com")
            .passwordHash("encoded")
            .firstName("Test")
            .lastName("User")
            .displayName("Test User")
            .roles(Set.of(role))
            .build();
        user.setId(userId);
    }

    // ---- Register ----

    @Test
    void register_shouldCreateUserAndReturnTokens() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@example.com");
        req.setPassword("Pass123!");
        req.setFirstName("New");
        req.setLastName("User");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(organizationRepository.findById(DEFAULT_ORG_ID)).thenReturn(Optional.of(org));
        when(roleRepository.findByNameAndOrganizationId("QA_ENGINEER", DEFAULT_ORG_ID)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("Pass123!")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtService.generateAccessToken(any(), any(), any(), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");

        AuthResponse response = authService.register(req);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrowOnDuplicateEmail() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("existing@example.com");
        req.setPassword("Pass123!");
        req.setFirstName("Dup");
        req.setLastName("User");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any());
    }

    // ---- Login ----

    @Test
    void login_shouldAuthenticateAndReturnTokens() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("Pass123!");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateAccessToken(any(), any(), any(), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");

        AuthResponse response = authService.login(req);

        assertEquals("access-token", response.getAccessToken());
        assertNotNull(response.getUserId());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_shouldThrowOnBadCredentials() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("wrong");

        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }

    // ---- Refresh ----

    @Test
    void refreshToken_shouldReturnNewTokensForValidRefreshToken() {
        when(jwtService.isTokenValid("valid-refresh")).thenReturn(true);
        when(jwtService.extractUserId("valid-refresh")).thenReturn(user.getId());
        when(userRepository.findByIdAndDeletedFalse(user.getId())).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(any(), any(), any(), any())).thenReturn("new-access");
        when(jwtService.generateRefreshToken(any())).thenReturn("new-refresh");

        AuthResponse response = authService.refreshToken("valid-refresh");

        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
    }

    @Test
    void refreshToken_shouldThrowOnInvalidToken() {
        when(jwtService.isTokenValid("expired-token")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.refreshToken("expired-token"));
    }

    @Test
    void refreshToken_shouldThrowForDeletedUser() {
        when(jwtService.isTokenValid("valid-refresh")).thenReturn(true);
        when(jwtService.extractUserId("valid-refresh")).thenReturn(user.getId());
        when(userRepository.findByIdAndDeletedFalse(user.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.refreshToken("valid-refresh"));
    }
}
