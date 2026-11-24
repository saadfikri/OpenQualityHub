package com.openqualityhub.integration;

import com.openqualityhub.dto.request.CreateTeamRequest;
import com.openqualityhub.dto.request.UpdateTeamRequest;
import com.openqualityhub.dto.response.TeamResponse;
import com.openqualityhub.exception.GlobalExceptionHandler.ApiError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Team Controller Integration Tests")
class TeamControllerIntegrationTest extends BaseIntegrationTest {

    private HttpHeaders adminHeaders;

    @BeforeEach
    void setUp() {
        adminHeaders = getAdminAuthHeaders();
    }

    // ---- Full CRUD Lifecycle ----

    @Test
    @DisplayName("CRUD lifecycle: create, get, update, list, delete, verify 404")
    void crud_fullLifecycle() {
        String teamName = "IT-Team-" + UUID.randomUUID().toString().substring(0, 8);

        // CREATE
        CreateTeamRequest createRequest = new CreateTeamRequest();
        createRequest.setName(teamName);
        createRequest.setDescription("Integration test team");

        HttpEntity<CreateTeamRequest> createEntity = new HttpEntity<>(createRequest, adminHeaders);
        ResponseEntity<TeamResponse> createResponse = restTemplate.postForEntity(
                "/api/v1/teams", createEntity, TeamResponse.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        UUID teamId = createResponse.getBody().getId();
        assertThat(teamId).isNotNull();
        assertThat(createResponse.getBody().getName()).isEqualTo(teamName);
        assertThat(createResponse.getBody().getDescription()).isEqualTo("Integration test team");
        assertThat(createResponse.getBody().getCreatedAt()).isNotNull();

        // GET BY ID
        ResponseEntity<TeamResponse> getResponse = restTemplate.exchange(
                "/api/v1/teams/" + teamId, HttpMethod.GET,
                new HttpEntity<>(adminHeaders), TeamResponse.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getName()).isEqualTo(teamName);

        // UPDATE
        String updatedName = "Updated-" + teamName;
        UpdateTeamRequest updateRequest = new UpdateTeamRequest();
        updateRequest.setName(updatedName);
        updateRequest.setDescription("Updated description");

        HttpEntity<UpdateTeamRequest> updateEntity = new HttpEntity<>(updateRequest, adminHeaders);
        ResponseEntity<TeamResponse> updateResponse = restTemplate.exchange(
                "/api/v1/teams/" + teamId, HttpMethod.PUT,
                updateEntity, TeamResponse.class);

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody()).isNotNull();
        assertThat(updateResponse.getBody().getName()).isEqualTo(updatedName);
        assertThat(updateResponse.getBody().getDescription()).isEqualTo("Updated description");

        // LIST (verify the team appears in the paginated results)
        ResponseEntity<String> listResponse = restTemplate.exchange(
                "/api/v1/teams?search=" + updatedName, HttpMethod.GET,
                new HttpEntity<>(adminHeaders), String.class);

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).contains(updatedName);

        // DELETE (soft delete)
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/v1/teams/" + teamId, HttpMethod.DELETE,
                new HttpEntity<>(adminHeaders), Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // VERIFY 404 after delete
        ResponseEntity<ApiError> afterDelete = restTemplate.exchange(
                "/api/v1/teams/" + teamId, HttpMethod.GET,
                new HttpEntity<>(adminHeaders), ApiError.class);

        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- Duplicate Name ----

    @Test
    @DisplayName("POST /api/v1/teams - should reject duplicate team name")
    void create_shouldRejectDuplicateName() {
        String teamName = "DupTeam-" + UUID.randomUUID().toString().substring(0, 8);

        CreateTeamRequest request = new CreateTeamRequest();
        request.setName(teamName);
        request.setDescription("First team");

        HttpEntity<CreateTeamRequest> entity = new HttpEntity<>(request, adminHeaders);

        // First creation should succeed
        ResponseEntity<TeamResponse> first = restTemplate.postForEntity(
                "/api/v1/teams", entity, TeamResponse.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Second creation with same name should fail
        CreateTeamRequest duplicate = new CreateTeamRequest();
        duplicate.setName(teamName);
        duplicate.setDescription("Duplicate team");

        HttpEntity<CreateTeamRequest> dupEntity = new HttpEntity<>(duplicate, adminHeaders);
        ResponseEntity<ApiError> second = restTemplate.postForEntity(
                "/api/v1/teams", dupEntity, ApiError.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(second.getBody()).isNotNull();
        assertThat(second.getBody().getMessage()).containsIgnoringCase("already exists");
    }

    // ---- Authentication Required ----

    @Test
    @DisplayName("POST /api/v1/teams - should require authentication")
    void create_shouldRequireAuthentication() {
        CreateTeamRequest request = new CreateTeamRequest();
        request.setName("Unauthenticated-Team-" + UUID.randomUUID().toString().substring(0, 8));
        request.setDescription("Should not be created");

        // No auth headers
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/teams", request, String.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("GET /api/v1/teams - should require authentication")
    void list_shouldRequireAuthentication() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/teams", String.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }
}
