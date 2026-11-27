package com.openqualityhub.controller;

import com.openqualityhub.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
@Tag(name = "Export", description = "CSV export for test runs and defects")
public class ExportController {

    private final ExportService exportService;

    @GetMapping("/test-runs")
    @PreAuthorize("hasAuthority('TEST_RUN_READ')")
    @Operation(summary = "Export test runs as CSV")
    public ResponseEntity<StreamingResponseBody> exportTestRuns(
            @RequestParam(required = false) UUID projectId) {
        StreamingResponseBody body = out -> exportService.exportTestRunsCsv(projectId, out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=test-runs.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
    }

    @GetMapping("/defects")
    @PreAuthorize("hasAuthority('DEFECT_READ')")
    @Operation(summary = "Export defects as CSV")
    public ResponseEntity<StreamingResponseBody> exportDefects(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) String status) {
        StreamingResponseBody body = out -> exportService.exportDefectsCsv(projectId, status, out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=defects.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
    }
}
