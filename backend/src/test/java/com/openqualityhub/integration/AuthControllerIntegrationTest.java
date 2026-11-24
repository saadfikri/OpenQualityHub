package com.openqualityhub.integration;

import com.openqualityhub.dto.request.LoginRequest;
import com.openqualityhub.dto.request.RefreshTokenRequest;
import com.openqualityhub.dto.request.RegisterRequest;
import com.openqualityhub.dto.response.AuthResponse;
import com.openqualityhub.exception.GlobalExceptionHandler.ApiError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Auth Controller Integration Tests")
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    private HttpHeaders adminHeaders;

    @BeforeEach
    void setUp() {
        adminHeaders = getAdminAuthHeaders();
    }

    // ---- Register ----

    @Test
    @DisplayName("POST /api/v1/auth/register - should create user and return tokens")
    void register_shouldCreateUserAndReturnTokens() {
        String uniqueEmail = "register-ok-" + UUID.randomUUID() + "@test.com";

        RegisterRequest request = new RegisterRequest();
        request.setEmail(uniqueEmail);
        request.setPassword("SecurePass123!");
        request.setFirstName("Test");
        request.setLastName("User");

        HttpEntity<RegisterRequest> entity = new HttpEntity<>(request, adminHeaders);
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/register", entity, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isNotBlank();
        assertThat(response.getBody().getRefreshToken()).isNotBlank();
        assertThat(response.getBody().getTokenType()).isEqualTo("Bearer");
        assertThat(response.getBody().getEmail()).isEqualTo(uniqueEmail);
        assertThat(response.getBody().getDisplayName()).isEqualTo("Test User");
        assertThat(response.getBody().getUserId()).isNotNull();
        assertThat(response.getBody().getOrganizationId()).isEqualTo(DEFAULT_ORG_ID);
        assertThat(response.getBody().getPermissions()).isNotEmpty();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - should reject duplicate email")
    void register_shouldRejectDuplicateEmail() {
        String uniqueEmail = "register-dup-" + UUID.randomUUID() + "@test.com";

        RegisterRequest request = new RegisterRequest();
        request.setEmail(uniqueEmail);
        request.setPassword("SecurePass123!");
        request.setFirstName("First");
        request.setLastName("User");

        // First registration should succeed
        HttpEntity<RegisterRequest> entity = new HttpEntity<>(request, adminHeaders);
        ResponseEntity<AuthResponse> first = restTemplate.postForEntity(
                "/api/v1/auth/register", entity, AuthResponse.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Second registration with the same email should fail
        RegisterRequest duplicate = new RegisterRequest();
        duplicate.setEmail(uniqueEmail);
        duplicate.setPassword("AnotherPass123!");
        duplicate.setFirstName("Second");
        duplicate.setLastName("User");

        HttpEntity<RegisterRequest> dupEntity = new HttpEntity<>(duplicate, adminHeaders);
        ResponseEntity<ApiError> second = restTemplate.postForEntity(
                "/api/v1/auth/register", dupEntity, ApiError.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(second.getBody()).isNotNull();
        assertThat(second.getBody().getMessage()).containsIgnoringCase("already registered");
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - should reject unauthenticated request")
    void register_shouldRequireAuthentication() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("noauth-" + UUID.randomUUID() + "@test.com");
        request.setPassword("SecurePass123!");
        request.setFirstName("No");
        request.setLastName("Auth");

        // No auth headers - should be rejected
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/register", request, String.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    // ---- Login ----

    @Test
    @DisplayName("POST /api/v1/auth/login - should return tokens for valid credentials")
    void login_shouldReturnTokens() {
        // Register a user first
        String uniqueEmail = "login-ok-" + UUID.randomUUID() + "@test.com";
        RegisterRequest regRequest = new RegisterRequest();
        regRequest.setEmail(uniqueEmail);
        regRequest.setPassword("LoginTestPass1!");
        regRequest.setFirstName("Login");
        regRequest.setLastName("Tester");

        HttpEntity<RegisterRequest> regEntity = new HttpEntity<>(regRequest, adminHeaders);
        restTemplate.postForEntity("/api/v1/auth/register", regEntity, AuthResponse.class);

        // Now login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(uniqueEmail);
        loginRequest.setPassword("LoginTestPass1!");

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login", loginRequest, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isNotBlank();
        assertThat(response.getBody().getRefreshToken()).isNotBlank();
        assertThat(response.getBody().getTokenType()).isEqualTo("Bearer");
        assertThat(response.getBody().getEmail()).isEqualTo(uniqueEmail);
        assertThat(response.getBody().getUserId()).isNotNull();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - should reject bad credentials")
    void login_shouldRejectBadCredentials() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nonexistent-" + UUID.randomUUID() + "@test.com");
        loginRequest.setPassword("WrongPassword1!");

        ResponseEntity<ApiError> response = restTemplate.postForEntity(
                "/api/v1/auth/login", loginRequest, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).containsIgnoringCase("invalid");
    }

    // ---- Refresh Token ----

    @Test
    @DisplayName("POST /api/v1/auth/refresh - should return new tokens")
    void refreshToken_shouldReturnNewTokens() {
        // Register and get tokens
        String uniqueEmail = "refresh-ok-" + UUID.randomUUID() + "@test.com";
        RegisterRequest regRequest = new RegisterRequest();
        regRequest.setEmail(uniqueEmail);
        regRequest.setPassword("RefreshTest1!");
        regRequest.setFirstName("Refresh");
        regRequest.setLastName("Tester");

        HttpEntity<RegisterRequest> regEntity = new HttpEntity<>(regRequest, adminHeaders);
        ResponseEntity<AuthResponse> regResponse = restTemplate.postForEntity(
                "/api/v1/auth/register", regEntity, AuthResponse.class);

        assertThat(regResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String refreshToken = regResponse.getBody().getRefreshToken();
        assertThat(refreshToken).isNotBlank();

        // Use the refresh token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(refreshToken);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/refresh", refreshRequest, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isNotBlank();
        assertThat(response.getBody().getRefreshToken()).isNotBlank();
        assertThat(response.getBody().getEmail()).isEqualTo(uniqueEmail);
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - should reject invalid refresh token")
    void refreshToken_shouldRejectInvalidToken() {
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken("invalid-token-that-should-be-rejected");

        ResponseEntity<ApiError> response = restTemplate.postForEntity(
                "/api/v1/auth/refresh", refreshRequest, ApiError.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED);
    }
}
