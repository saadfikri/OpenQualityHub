package com.openqualityhub.integration;

import com.openqualityhub.dto.request.LoginRequest;
import com.openqualityhub.dto.request.RegisterRequest;
import com.openqualityhub.dto.response.AuthResponse;
import com.openqualityhub.entity.Organization;
import com.openqualityhub.entity.Role;
import com.openqualityhub.entity.User;
import com.openqualityhub.repository.OrganizationRepository;
import com.openqualityhub.repository.RoleRepository;
import com.openqualityhub.repository.UserRepository;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for integration tests using Testcontainers with a real PostgreSQL database.
 * All Flyway migrations run against the container, providing a production-equivalent schema.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@Testcontainers(disabledWithoutDocker = true)
public abstract class BaseIntegrationTest {

    protected static final String ADMIN_EMAIL = "admin@integration.test";
    protected static final String ADMIN_PASSWORD = "TestAdmin123!";
    protected static final UUID DEFAULT_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    protected static final UUID ADMIN_ROLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("oqh_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected OrganizationRepository organizationRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    /**
     * Ensures a test admin user exists in the database.
     * This user has the ADMIN role with all permissions, enabling
     * full API access for integration tests.
     */
    protected void ensureAdminUserExists() {
        if (userRepository.findByEmail(ADMIN_EMAIL).isPresent()) {
            return;
        }

        Organization org = organizationRepository.findById(DEFAULT_ORG_ID)
                .orElseThrow(() -> new IllegalStateException("Default organization not found; Flyway migrations may have failed"));

        Role adminRole = roleRepository.findById(ADMIN_ROLE_ID)
                .orElseThrow(() -> new IllegalStateException("ADMIN role not found; Flyway migrations may have failed"));

        User admin = User.builder()
                .organization(org)
                .email(ADMIN_EMAIL)
                .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
                .firstName("Admin")
                .lastName("Tester")
                .displayName("Admin Tester")
                .roles(Set.of(adminRole))
                .build();

        userRepository.save(admin);
    }

    /**
     * Returns HTTP headers with a Bearer token for the test admin user.
     */
    protected HttpHeaders getAdminAuthHeaders() {
        ensureAdminUserExists();
        return getAuthHeaders(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    /**
     * Authenticates with the given credentials and returns HTTP headers
     * containing the Bearer token.
     */
    protected HttpHeaders getAuthHeaders(String email, String password) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login", loginRequest, AuthResponse.class);

        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Login should succeed for %s", email)
                .isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(response.getBody().getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Logs in and returns the full AuthResponse (access token, refresh token, user info).
     */
    protected AuthResponse login(String email, String password) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login", loginRequest, AuthResponse.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    /**
     * Registers a new user (requires an existing authenticated session),
     * then returns auth headers for the newly registered user.
     */
    protected HttpHeaders registerAndLogin(String email, String password,
                                           String firstName, String lastName,
                                           HttpHeaders existingAuthHeaders) {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);
        registerRequest.setFirstName(firstName);
        registerRequest.setLastName(lastName);

        HttpEntity<RegisterRequest> entity = new HttpEntity<>(registerRequest, existingAuthHeaders);
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/register", entity, AuthResponse.class);

        assertThat(response.getStatusCode().value())
                .as("Registration should return 201 for %s", email)
                .isEqualTo(201);

        return getAuthHeaders(email, password);
    }
}
