package com.openqualityhub.controller;

import com.openqualityhub.dto.request.*;
import com.openqualityhub.dto.response.*;
import com.openqualityhub.service.TestDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/test-data")
@RequiredArgsConstructor
@Tag(name = "Test Data", description = "Test data template management, synthetic data generation, and profiles")
public class TestDataController {

    private final TestDataService testDataService;

    // ---- Templates ----

    @PostMapping("/templates")
    @Operation(summary = "Create a new test data template")
    @PreAuthorize("hasAuthority('TEST_DATA_WRITE')")
    public ResponseEntity<TestDataTemplateResponse> createTemplate(
            @Valid @RequestBody CreateTestDataTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(testDataService.createTemplate(request));
    }

    @PutMapping("/templates/{id}")
    @Operation(summary = "Update a test data template")
    @PreAuthorize("hasAuthority('TEST_DATA_WRITE')")
    public ResponseEntity<TestDataTemplateResponse> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTestDataTemplateRequest request) {
        return ResponseEntity.ok(testDataService.updateTemplate(id, request));
    }

    @GetMapping("/templates/{id}")
    @Operation(summary = "Get a test data template by ID with fields and profiles")
    @PreAuthorize("hasAuthority('TEST_DATA_READ')")
    public ResponseEntity<TestDataTemplateDetailResponse> getTemplate(@PathVariable UUID id) {
        return ResponseEntity.ok(testDataService.getTemplateById(id));
    }

    @GetMapping("/templates")
    @Operation(summary = "List all test data templates")
    @PreAuthorize("hasAuthority('TEST_DATA_READ')")
    public ResponseEntity<Page<TestDataTemplateResponse>> getTemplates(
            @RequestParam(required = false) UUID projectId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(testDataService.getAllTemplates(projectId, pageable));
    }

    @DeleteMapping("/templates/{id}")
    @Operation(summary = "Delete a test data template")
    @PreAuthorize("hasAuthority('TEST_DATA_WRITE')")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        testDataService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Profiles ----

    @PostMapping("/profiles")
    @Operation(summary = "Create a data profile for a template")
    @PreAuthorize("hasAuthority('TEST_DATA_WRITE')")
    public ResponseEntity<TestDataProfileResponse> createProfile(
            @Valid @RequestBody CreateTestDataProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(testDataService.createProfile(request));
    }

    @PutMapping("/profiles/{id}")
    @Operation(summary = "Update a data profile")
    @PreAuthorize("hasAuthority('TEST_DATA_WRITE')")
    public ResponseEntity<TestDataProfileResponse> updateProfile(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTestDataProfileRequest request) {
        return ResponseEntity.ok(testDataService.updateProfile(id, request));
    }

    @DeleteMapping("/profiles/{id}")
    @Operation(summary = "Delete a data profile")
    @PreAuthorize("hasAuthority('TEST_DATA_WRITE')")
    public ResponseEntity<Void> deleteProfile(@PathVariable UUID id) {
        testDataService.deleteProfile(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Generation ----

    @PostMapping("/generate")
    @Operation(summary = "Generate synthetic test data based on template and profile")
    @PreAuthorize("hasAuthority('TEST_DATA_WRITE')")
    public ResponseEntity<TestDataGenerationResponse> generate(
            @Valid @RequestBody GenerateTestDataRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(testDataService.generate(request));
    }

    @GetMapping("/generations/{id}")
    @Operation(summary = "Get a generation record by ID")
    @PreAuthorize("hasAuthority('TEST_DATA_READ')")
    public ResponseEntity<TestDataGenerationResponse> getGeneration(@PathVariable UUID id) {
        return ResponseEntity.ok(testDataService.getGeneration(id));
    }

    @GetMapping("/templates/{templateId}/generations")
    @Operation(summary = "List generation history for a template")
    @PreAuthorize("hasAuthority('TEST_DATA_READ')")
    public ResponseEntity<Page<TestDataGenerationResponse>> getGenerations(
            @PathVariable UUID templateId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(testDataService.getGenerationsByTemplate(templateId, pageable));
    }

    @GetMapping("/generations/{id}/download")
    @Operation(summary = "Download generated test data as JSON or CSV")
    @PreAuthorize("hasAuthority('TEST_DATA_READ')")
    public ResponseEntity<byte[]> downloadGeneration(@PathVariable UUID id) {
        byte[] data = testDataService.downloadGeneration(id);
        TestDataGenerationResponse gen = testDataService.getGeneration(id);
        boolean isCsv = "CSV".equals(gen.getExportFormat());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"test-data-" + id + (isCsv ? ".csv" : ".json") + "\"")
                .contentType(isCsv ? MediaType.parseMediaType("text/csv") : MediaType.APPLICATION_JSON)
                .body(data);
    }

    // ---- Dashboard ----

    @GetMapping("/dashboard")
    @Operation(summary = "Get test data management dashboard statistics")
    @PreAuthorize("hasAuthority('TEST_DATA_READ')")
    public ResponseEntity<TestDataDashboardResponse> getDashboard() {
        return ResponseEntity.ok(testDataService.getDashboard());
    }
}
