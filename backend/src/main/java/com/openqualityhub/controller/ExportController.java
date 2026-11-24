package com.openqualityhub.controller;

import com.openqualityhub.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
@Tag(name = "Export", description = "CSV export for test runs and defects")
public class ExportController {

    private final ExportService exportService;

    @GetMapping("/test-runs")
    @Operation(summary = "Export test runs as CSV")
    public ResponseEntity<byte[]> exportTestRuns(
            @RequestParam(required = false) UUID projectId) {
        byte[] csv = exportService.exportTestRunsCsv(projectId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=test-runs.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/defects")
    @Operation(summary = "Export defects as CSV")
    public ResponseEntity<byte[]> exportDefects(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) String status) {
        byte[] csv = exportService.exportDefectsCsv(projectId, status);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=defects.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
