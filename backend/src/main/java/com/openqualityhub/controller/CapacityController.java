package com.openqualityhub.controller;

import com.openqualityhub.dto.request.CreateCapacityAllocationRequest;
import com.openqualityhub.dto.response.CapacityOverviewResponse;
import com.openqualityhub.service.CapacityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/capacity")
@RequiredArgsConstructor
@Tag(name = "Capacity Planning", description = "Team workload and capacity management")
public class CapacityController {

    private final CapacityService capacityService;

    @PostMapping
    @Operation(summary = "Create or update a capacity allocation")
    public ResponseEntity<Void> createOrUpdate(@Valid @RequestBody CreateCapacityAllocationRequest request) {
        capacityService.createOrUpdate(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/overview")
    @Operation(summary = "Get capacity overview across all teams")
    public ResponseEntity<CapacityOverviewResponse> getOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return ResponseEntity.ok(capacityService.getOverview(weekStart));
    }
}
