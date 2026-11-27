package com.openqualityhub.controller;

import com.openqualityhub.dto.response.*;
import com.openqualityhub.service.MaestroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/maestro")
@RequiredArgsConstructor
@Tag(name = "Maestro", description = "Maestro mobile testing integration - device matrix, flow analytics, result import")
public class MaestroController {

    private final MaestroService maestroService;

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('TEST_RUN_WRITE')")
    @Operation(summary = "Import Maestro JSON test results")
    public ResponseEntity<TestRunResponse> importResults(
            @RequestParam UUID projectId,
            @RequestParam String name,
            @RequestParam(required = false) String environment,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(maestroService.importMaestroResults(projectId, name, environment, file));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('TEST_RUN_READ')")
    @Operation(summary = "Get Maestro mobile testing dashboard")
    public ResponseEntity<MaestroDashboardResponse> getDashboard() {
        return ResponseEntity.ok(maestroService.getDashboard());
    }

    @GetMapping("/device-matrix")
    @PreAuthorize("hasAuthority('TEST_RUN_READ')")
    @Operation(summary = "Get device compatibility matrix")
    public ResponseEntity<MaestroDeviceMatrixResponse> getDeviceMatrix() {
        return ResponseEntity.ok(maestroService.getDeviceMatrix());
    }

    @GetMapping("/flow-analytics")
    @PreAuthorize("hasAuthority('TEST_RUN_READ')")
    @Operation(summary = "Get flow-level analytics and top failing flows")
    public ResponseEntity<MaestroFlowAnalyticsResponse> getFlowAnalytics() {
        return ResponseEntity.ok(maestroService.getFlowAnalytics());
    }

    @GetMapping("/flows")
    @PreAuthorize("hasAuthority('TEST_RUN_READ')")
    @Operation(summary = "Get paginated flow results with optional platform filter")
    public ResponseEntity<Page<MaestroFlowResultResponse>> getFlowResults(
            @RequestParam(required = false) String platform,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(maestroService.getFlowResults(platform, pageable));
    }

    @GetMapping("/test-runs/{testRunId}/flows")
    @PreAuthorize("hasAuthority('TEST_RUN_READ')")
    @Operation(summary = "Get Maestro flow results for a specific test run")
    public ResponseEntity<List<MaestroFlowResultResponse>> getFlowsByTestRun(@PathVariable UUID testRunId) {
        return ResponseEntity.ok(maestroService.getFlowResultsByTestRun(testRunId));
    }
}
