package com.openqualityhub.dto;

import com.openqualityhub.dto.request.CreateProjectRequest;
import com.openqualityhub.dto.request.CreateTeamRequest;
import com.openqualityhub.dto.request.RegisterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ---- RegisterRequest ----

    @Test
    void registerRequest_validRequest_shouldPass() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setPassword("SecurePass123");
        request.setFirstName("John");
        request.setLastName("Doe");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void registerRequest_blankEmail_shouldFail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("");
        request.setPassword("SecurePass123");
        request.setFirstName("John");
        request.setLastName("Doe");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void registerRequest_invalidEmail_shouldFail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("not-an-email");
        request.setPassword("SecurePass123");
        request.setFirstName("John");
        request.setLastName("Doe");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void registerRequest_shortPassword_shouldFail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setPassword("short");
        request.setFirstName("John");
        request.setLastName("Doe");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void registerRequest_blankPassword_shouldFail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setPassword("");
        request.setFirstName("John");
        request.setLastName("Doe");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void registerRequest_blankFirstName_shouldFail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setPassword("SecurePass123");
        request.setFirstName("");
        request.setLastName("Doe");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("firstName")));
    }

    @Test
    void registerRequest_blankLastName_shouldFail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setPassword("SecurePass123");
        request.setFirstName("John");
        request.setLastName("");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("lastName")));
    }

    @Test
    void registerRequest_nullFields_shouldFail() {
        RegisterRequest request = new RegisterRequest();
        // All fields null

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertEquals(4, violations.size());
    }

    // ---- CreateTeamRequest ----

    @Test
    void createTeamRequest_validRequest_shouldPass() {
        CreateTeamRequest request = new CreateTeamRequest();
        request.setName("QA Team");
        request.setDescription("Quality Assurance team");

        Set<ConstraintViolation<CreateTeamRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void createTeamRequest_blankName_shouldFail() {
        CreateTeamRequest request = new CreateTeamRequest();
        request.setName("");

        Set<ConstraintViolation<CreateTeamRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void createTeamRequest_nullName_shouldFail() {
        CreateTeamRequest request = new CreateTeamRequest();
        request.setName(null);

        Set<ConstraintViolation<CreateTeamRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void createTeamRequest_nameTooLong_shouldFail() {
        CreateTeamRequest request = new CreateTeamRequest();
        request.setName("A".repeat(256));

        Set<ConstraintViolation<CreateTeamRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void createTeamRequest_descriptionTooLong_shouldFail() {
        CreateTeamRequest request = new CreateTeamRequest();
        request.setName("Valid Name");
        request.setDescription("A".repeat(1001));

        Set<ConstraintViolation<CreateTeamRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("description")));
    }

    // ---- CreateProjectRequest ----

    @Test
    void createProjectRequest_validRequest_shouldPass() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("Web App");
        request.setDescription("Main web application");
        request.setTeamId(UUID.randomUUID());

        Set<ConstraintViolation<CreateProjectRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void createProjectRequest_blankName_shouldFail() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("");
        request.setTeamId(UUID.randomUUID());

        Set<ConstraintViolation<CreateProjectRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void createProjectRequest_nullTeamId_shouldFail() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("Project");
        request.setTeamId(null);

        Set<ConstraintViolation<CreateProjectRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("teamId")));
    }

    @Test
    void createProjectRequest_nameTooLong_shouldFail() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("A".repeat(256));
        request.setTeamId(UUID.randomUUID());

        Set<ConstraintViolation<CreateProjectRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void createProjectRequest_allMissing_shouldFail() {
        CreateProjectRequest request = new CreateProjectRequest();
        // name null, teamId null

        Set<ConstraintViolation<CreateProjectRequest>> violations = validator.validate(request);
        assertEquals(2, violations.size());
    }
}
