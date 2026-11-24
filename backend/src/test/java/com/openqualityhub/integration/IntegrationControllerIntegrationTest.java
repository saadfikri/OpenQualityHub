package com.openqualityhub.integration;

import com.openqualityhub.dto.request.CreateIntegrationRequest;
import com.openqualityhub.dto.response.IntegrationConfigResponse;
import com.openqualityhub.exception.GlobalExceptionHandler.ApiError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Integration Controller Integration Tests")
class IntegrationControllerIntegrationTest extends BaseIntegrationTest {

    private HttpHeaders adminHeaders;

    @BeforeEach
    void setUp() {
        adminHeaders = getAdminAuthHeaders();
    }

    // ---- Full CRUD Lifecycle ----

    @Test
    @DisplayName("CRUD lifecycle: create, get, list, delete, verify 404")
    void crud_fullLifecycle() {
        String integrationName = "IT-Integration-" + UUID.randomUUID().toString().substring(0, 8);

        // CREATE
        CreateIntegrationRequest createRequest = new CreateIntegrationRequest();
        createRequest.setName(integrationName);
        createRequest.setDescription("Integration test webhook");
        createRequest.setProvider("GITHUB");
        createRequest.setWebhookSecret("test-secret-123");

        HttpEntity<CreateIntegrationRequest> createEntity = new HttpEntity<>(createRequest, adminHeaders);
        ResponseEntity<IntegrationConfigResponse> createResponse = restTemplate.postForEntity(
                "/api/v1/integrations", createEntity, IntegrationConfigResponse.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        UUID integrationId = createResponse.getBody().getId();
        assertThat(integrationId).isNotNull();
        assertThat(createResponse.getBody().getName()).isEqualTo(integrationName);
        assertThat(createResponse.getBody().getDescription()).isEqualTo("Integration test webhook");
        assertThat(createResponse.getBody().getProvider()).isEqualTo("GITHUB");
        assertThat(createResponse.getBody().isEnabled()).isTrue();
        assertThat(createResponse.getBody().isHasSecret()).isTrue();
        assertThat(createResponse.getBody().getCreatedAt()).isNotNull();

        // GET BY ID
        ResponseEntity<IntegrationConfigResponse> getResponse = restTemplate.exchange(
                "/api/v1/integrations/" + integrationId, HttpMethod.GET,
                new HttpEntity<>(adminHeaders), IntegrationConfigResponse.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getName()).isEqualTo(integrationName);
        assertThat(getResponse.getBody().getProvider()).isEqualTo("GITHUB");

        // LIST
        ResponseEntity<String> listResponse = restTemplate.exchange(
                "/api/v1/integrations", HttpMethod.GET,
                new HttpEntity<>(adminHeaders), String.class);

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).contains(integrationName);

        // LIST by provider
        ResponseEntity<String> providerListResponse = restTemplate.exchange(
                "/api/v1/integrations?provider=GITHUB", HttpMethod.GET,
                new HttpEntity<>(adminHeaders), String.class);

        assertThat(providerListResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(providerListResponse.getBody()).contains(integrationName);

        // GET WEBHOOK URL
        ResponseEntity<String> webhookUrlResponse = restTemplate.exchange(
                "/api/v1/integrations/" + integrationId + "/webhook-url", HttpMethod.GET,
                new HttpEntity<>(adminHeaders), String.class);

        assertThat(webhookUrlResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(webhookUrlResponse.getBody()).contains(integrationId.toString());

        // DELETE (soft delete)
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/v1/integrations/" + integrationId, HttpMethod.DELETE,
                new HttpEntity<>(adminHeaders), Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // VERIFY 404 after delete
        ResponseEntity<ApiError> afterDelete = restTemplate.exchange(
                "/api/v1/integrations/" + integrationId, HttpMethod.GET,
                new HttpEntity<>(adminHeaders), ApiError.class);

        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- Duplicate Name ----

    @Test
    @DisplayName("POST /api/v1/integrations - should reject duplicate name")
    void create_shouldRejectDuplicateName() {
        String integrationName = "DupIntegration-" + UUID.randomUUID().toString().substring(0, 8);

        CreateIntegrationRequest request = new CreateIntegrationRequest();
        request.setName(integrationName);
        request.setDescription("First integration");
        request.setProvider("JENKINS");

        HttpEntity<CreateIntegrationRequest> entity = new HttpEntity<>(request, adminHeaders);

        // First creation should succeed
        ResponseEntity<IntegrationConfigResponse> first = restTemplate.postForEntity(
                "/api/v1/integrations", entity, IntegrationConfigResponse.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Second creation with same name should fail
        CreateIntegrationRequest duplicate = new CreateIntegrationRequest();
        duplicate.setName(integrationName);
        duplicate.setDescription("Duplicate integration");
        duplicate.setProvider("GITLAB");

        HttpEntity<CreateIntegrationRequest> dupEntity = new HttpEntity<>(duplicate, adminHeaders);
        ResponseEntity<ApiError> second = restTemplate.postForEntity(
                "/api/v1/integrations", dupEntity, ApiError.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(second.getBody()).isNotNull();
        assertThat(second.getBody().getMessage()).containsIgnoringCase("already exists");
    }

    // ---- RBAC: QA_ENGINEER cannot write ----

    @Test
    @DisplayName("POST /api/v1/integrations - QA_ENGINEER should be forbidden from creating")
    void create_shouldForbidQaEngineer() {
        // Register a QA_ENGINEER user (default role for new registrations)
        String qaEmail = "qa-engineer-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        HttpHeaders qaHeaders = registerAndLogin(qaEmail, "QaEngPass123!", "QA", "Engineer", adminHeaders);

        CreateIntegrationRequest request = new CreateIntegrationRequest();
        request.setName("ForbiddenIntegration-" + UUID.randomUUID().toString().substring(0, 8));
        request.setDescription("Should be forbidden");
        request.setProvider("GENERIC");

        HttpEntity<CreateIntegrationRequest> entity = new HttpEntity<>(request, qaHeaders);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/integrations", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ---- Authentication Required ----

    @Test
    @DisplayName("GET /api/v1/integrations - should require authentication")
    void list_shouldRequireAuthentication() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/integrations", String.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    // ---- Dashboard ----

    @Test
    @DisplayName("GET /api/v1/integrations/dashboard - should return dashboard data")
    void dashboard_shouldReturnStats() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/integrations/dashboard", HttpMethod.GET,
                new HttpEntity<>(adminHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("totalIntegrations");
        assertThat(response.getBody()).contains("activeIntegrations");
    }
}
