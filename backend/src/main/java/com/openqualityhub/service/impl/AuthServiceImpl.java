package com.openqualityhub.service.impl;

import com.openqualityhub.dto.request.LoginRequest;
import com.openqualityhub.dto.request.RegisterRequest;
import com.openqualityhub.dto.response.AuthResponse;
import com.openqualityhub.entity.Organization;
import com.openqualityhub.entity.Role;
import com.openqualityhub.entity.User;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.repository.OrganizationRepository;
import com.openqualityhub.repository.RoleRepository;
import com.openqualityhub.repository.UserRepository;
import com.openqualityhub.security.JwtService;
import com.openqualityhub.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final UUID DEFAULT_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        Organization org = organizationRepository.findById(DEFAULT_ORG_ID)
            .orElseThrow(() -> new ResourceNotFoundException("Organization", DEFAULT_ORG_ID));

        Role defaultRole = roleRepository.findByNameAndOrganizationId("QA_ENGINEER", DEFAULT_ORG_ID)
            .orElseThrow(() -> new ResourceNotFoundException("Role", "QA_ENGINEER"));

        User user = User.builder()
            .organization(org)
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .displayName(request.getFirstName() + " " + request.getLastName())
            .roles(Set.of(defaultRole))
            .build();

        user = userRepository.save(user);
        log.info("Registered user: {} ({})", user.getEmail(), user.getId());

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        UUID userId = jwtService.extractUserId(refreshToken);
        User user = userRepository.findByIdAndDeletedFalse(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        Set<String> permissions = user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(p -> p.getName())
            .collect(Collectors.toSet());

        String accessToken = jwtService.generateAccessToken(
            user.getId(), user.getEmail(), user.getOrganization().getId().toString(), permissions
        );
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .userId(user.getId())
            .email(user.getEmail())
            .displayName(user.getDisplayName())
            .organizationId(user.getOrganization().getId())
            .permissions(permissions)
            .build();
    }
}
