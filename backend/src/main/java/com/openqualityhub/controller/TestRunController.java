package com.openqualityhub.controller;

import com.openqualityhub.dto.response.TestRunDetailResponse;
import com.openqualityhub.dto.response.TestRunResponse;
import com.openqualityhub.service.TestRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/test-runs")
@RequiredArgsConstructor
@Tag(name = "Test Runs", description = "Test result import and management")
public class TestRunController {

    private final TestRunService testRunService;

    @PostMapping(value = "/import/junit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import JUnit XML test results")
    public ResponseEntity<TestRunResponse> importJUnit(
            @RequestParam UUID projectId,
            @RequestParam String name,
            @RequestParam(required = false) String environment,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(testRunService.importJUnitResults(projectId, name, environment, file));
    }

    @PostMapping(value = "/import/playwright", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import Playwright JSON test results")
    public ResponseEntity<TestRunResponse> importPlaywright(
            @RequestParam UUID projectId,
            @RequestParam String name,
            @RequestParam(required = false) String environment,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(testRunService.importPlaywrightResults(projectId, name, environment, file));
    }

    @PostMapping(value = "/import/webdriverio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import WebdriverIO JSON test results")
    public ResponseEntity<TestRunResponse> importWebdriverIO(
            @RequestParam UUID projectId,
            @RequestParam String name,
            @RequestParam(required = false) String environment,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(testRunService.importWebdriverIOResults(projectId, name, environment, file));
    }

    @PostMapping(value = "/import/cypress", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import Cypress mochawesome JSON test results")
    public ResponseEntity<TestRunResponse> importCypress(
            @RequestParam UUID projectId,
            @RequestParam String name,
            @RequestParam(required = false) String environment,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(testRunService.importCypressResults(projectId, name, environment, file));
    }

    @PostMapping(value = "/import/testng", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import TestNG XML test results")
    public ResponseEntity<TestRunResponse> importTestNG(
            @RequestParam UUID projectId,
            @RequestParam String name,
            @RequestParam(required = false) String environment,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(testRunService.importTestNGResults(projectId, name, environment, file));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a test run with all test cases")
    public ResponseEntity<TestRunDetailResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(testRunService.getById(id));
    }

    @GetMapping
    @Operation(summary = "Get all test runs with pagination")
    public ResponseEntity<Page<TestRunResponse>> getAll(
            @RequestParam(required = false) UUID projectId,
            @PageableDefault(size = 20, sort = "executedAt") Pageable pageable) {
        if (projectId != null) {
            return ResponseEntity.ok(testRunService.getByProjectId(projectId, pageable));
        }
        return ResponseEntity.ok(testRunService.getAll(pageable));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a test run")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        testRunService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
