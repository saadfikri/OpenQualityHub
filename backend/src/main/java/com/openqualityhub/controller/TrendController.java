package com.openqualityhub.controller;

import com.openqualityhub.dto.response.DefectTrendResponse;
import com.openqualityhub.dto.response.PassRateTrendResponse;
import com.openqualityhub.dto.response.TrendPointResponse;
import com.openqualityhub.service.TrendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trends")
@RequiredArgsConstructor
@Tag(name = "Trends", description = "Trend analytics for quality metrics")
public class TrendController {

    private final TrendService trendService;

    @GetMapping("/pass-rate")
    @PreAuthorize("hasAuthority('PROJECT_READ')")
    @Operation(summary = "Get pass rate trend", description = "Returns daily average pass rate over the specified period")
    public ResponseEntity<PassRateTrendResponse> getPassRateTrend(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) UUID projectId) {
        return ResponseEntity.ok(trendService.getPassRateTrend(days, projectId));
    }

    @GetMapping("/defects")
    @PreAuthorize("hasAuthority('PROJECT_READ')")
    @Operation(summary = "Get defect trend", description = "Returns daily defect opened vs resolved counts")
    public ResponseEntity<DefectTrendResponse> getDefectTrend(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(trendService.getDefectTrend(days));
    }

    @GetMapping("/test-volume")
    @PreAuthorize("hasAuthority('PROJECT_READ')")
    @Operation(summary = "Get test volume trend", description = "Returns daily test execution volume")
    public ResponseEntity<TrendPointResponse> getTestVolumeTrend(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) UUID projectId) {
        return ResponseEntity.ok(trendService.getTestVolumeTrend(days, projectId));
    }

    @GetMapping("/flaky")
    @PreAuthorize("hasAuthority('PROJECT_READ')")
    @Operation(summary = "Get flaky test trend", description = "Returns daily flaky test count trend")
    public ResponseEntity<TrendPointResponse> getFlakyTrend(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) UUID projectId) {
        return ResponseEntity.ok(trendService.getFlakyTrend(days, projectId));
    }
}
