package com.openqualityhub.controller;

import com.openqualityhub.dto.response.FlakyTestResponse;
import com.openqualityhub.dto.response.FlakyTestSummaryResponse;
import com.openqualityhub.service.FlakyTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/flaky-tests")
@RequiredArgsConstructor
@Tag(name = "Flaky Tests", description = "Flaky test analysis and tracking")
public class FlakyTestController {

    private final FlakyTestService flakyTestService;

    @GetMapping("/summary")
    @Operation(summary = "Get flaky test summary across all projects")
    public ResponseEntity<FlakyTestSummaryResponse> getSummary() {
        return ResponseEntity.ok(flakyTestService.getSummary());
    }

    @GetMapping
    @Operation(summary = "List flaky tests")
    public ResponseEntity<Page<FlakyTestResponse>> getFlakyTests(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(defaultValue = "true") boolean flakyOnly,
            @PageableDefault(size = 20) Pageable pageable) {
        if (flakyOnly) {
            return ResponseEntity.ok(flakyTestService.getFlakyTests(projectId, pageable));
        }
        return ResponseEntity.ok(flakyTestService.getAllTestIdentities(projectId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get test identity details")
    public ResponseEntity<FlakyTestResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(flakyTestService.getById(id));
    }
}
