package com.openqualityhub.integration;

import com.openqualityhub.dto.request.CreateProjectRequest;
import com.openqualityhub.dto.request.CreateTeamRequest;
import com.openqualityhub.dto.request.UpdateProjectRequest;
import com.openqualityhub.dto.response.ProjectResponse;
import com.openqualityhub.dto.response.TeamResponse;
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

@DisplayName("Project Controller Integration Tests")
class ProjectControllerIntegrationTest extends BaseIntegrationTest {

    private HttpHeaders adminHeaders;

    @BeforeEach
    void setUp() {
        adminHeaders = getAdminAuthHeaders();
    }

    /**
     * Helper: creates a team and returns its ID.
     */
    private UUID createTeam(String suffix) {
        CreateTeamRequest teamRequest = new CreateTeamRequest();
        teamRequest.setName("ProjTestTeam-" + suffix);
        teamRequest.setDescription("Team for project tests");

        HttpEntity<CreateTeamRequest> entity = new HttpEntity<>(teamRequest, adminHeaders);
        ResponseEntity<TeamResponse> response = restTemplate.postForEntity(
                "/api/v1/teams", entity, TeamResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().getId();
    }

    // ---- Full CRUD Lifecycle ----

    @Test
    @DisplayName("CRUD lifecycle: create team, create project, get, update, delete, verify 404")
    void crud_fullLifecycle() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UUID teamId = createTeam(suffix);
        String projectName = "IT-Project-" + suffix;

        // CREATE
        CreateProjectRequest createRequest = new CreateProjectRequest();
        createRequest.setName(projectName);
        createRequest.setDescription("Integration test project");
        createRequest.setTeamId(teamId);

        HttpEntity<CreateProjectRequest> createEntity = new HttpEntity<>(createRequest, adminHeaders);
        ResponseEntity<ProjectResponse> createResponse = restTemplate.postForEntity(
                "/api/v1/projects", createEntity, ProjectResponse.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        UUID projectId = createResponse.getBody().getId();
        assertThat(projectId).isNotNull();
        assertThat(createResponse.getBody().getName()).isEqualTo(projectName);
        assertThat(createResponse.getBody().getDescription()).isEqualTo("Integration test project");
        assertThat(createResponse.getBody().getTeamId()).isEqualTo(teamId);
        assertThat(createResponse.getBody().getStatus()).isNotNull();
        assertThat(createResponse.getBody().getCreatedAt()).isNotNull();

        // GET BY ID
        ResponseEntity<ProjectResponse> getResponse = restTemplate.exchange(
                "/api/v1/projects/" + projectId, HttpMethod.GET,
                new HttpEntity<>(adminHeaders), ProjectResponse.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getName()).isEqualTo(projectName);
        assertThat(getResponse.getBody().getTeamId()).isEqualTo(teamId);

        // UPDATE
        String updatedName = "Updated-" + projectName;
        UpdateProjectRequest updateRequest = new UpdateProjectRequest();
        updateRequest.setName(updatedName);
        updateRequest.setDescription("Updated project description");

        HttpEntity<UpdateProjectRequest> updateEntity = new HttpEntity<>(updateRequest, adminHeaders);
        ResponseEntity<ProjectResponse> updateResponse = restTemplate.exchange(
                "/api/v1/projects/" + projectId, HttpMethod.PUT,
                updateEntity, ProjectResponse.class);

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody()).isNotNull();
        assertThat(updateResponse.getBody().getName()).isEqualTo(updatedName);
        assertThat(updateResponse.getBody().getDescription()).isEqualTo("Updated project description");

        // LIST by team
        ResponseEntity<String> listResponse = restTemplate.exchange(
                "/api/v1/projects?teamId=" + teamId, HttpMethod.GET,
                new HttpEntity<>(adminHeaders), String.class);

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).contains(updatedName);

        // DELETE (soft delete)
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/v1/projects/" + projectId, HttpMethod.DELETE,
                new HttpEntity<>(adminHeaders), Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // VERIFY 404 after delete
        ResponseEntity<ApiError> afterDelete = restTemplate.exchange(
                "/api/v1/projects/" + projectId, HttpMethod.GET,
                new HttpEntity<>(adminHeaders), ApiError.class);

        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- Missing Team ----

    @Test
    @DisplayName("POST /api/v1/projects - should reject missing/nonexistent team")
    void create_shouldRejectMissingTeam() {
        UUID fakeTeamId = UUID.randomUUID();

        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("OrphanProject-" + UUID.randomUUID().toString().substring(0, 8));
        request.setDescription("Project with nonexistent team");
        request.setTeamId(fakeTeamId);

        HttpEntity<CreateProjectRequest> entity = new HttpEntity<>(request, adminHeaders);
        ResponseEntity<ApiError> response = restTemplate.postForEntity(
                "/api/v1/projects", entity, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).containsIgnoringCase("Team");
    }

    // ---- Duplicate Name in Same Team ----

    @Test
    @DisplayName("POST /api/v1/projects - should reject duplicate name within same team")
    void create_shouldRejectDuplicateNameInTeam() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UUID teamId = createTeam("dup-" + suffix);
        String projectName = "DupProject-" + suffix;

        CreateProjectRequest request = new CreateProjectRequest();
        request.setName(projectName);
        request.setDescription("First project");
        request.setTeamId(teamId);

        HttpEntity<CreateProjectRequest> entity = new HttpEntity<>(request, adminHeaders);

        // First should succeed
        ResponseEntity<ProjectResponse> first = restTemplate.postForEntity(
                "/api/v1/projects", entity, ProjectResponse.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Second with same name in same team should fail
        CreateProjectRequest duplicate = new CreateProjectRequest();
        duplicate.setName(projectName);
        duplicate.setDescription("Duplicate project");
        duplicate.setTeamId(teamId);

        HttpEntity<CreateProjectRequest> dupEntity = new HttpEntity<>(duplicate, adminHeaders);
        ResponseEntity<ApiError> second = restTemplate.postForEntity(
                "/api/v1/projects", dupEntity, ApiError.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(second.getBody()).isNotNull();
        assertThat(second.getBody().getMessage()).containsIgnoringCase("already exists");
    }

    // ---- Authentication Required ----

    @Test
    @DisplayName("GET /api/v1/projects - should require authentication")
    void list_shouldRequireAuthentication() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/projects", String.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }
}
