package com.openqualityhub.controller;

import com.openqualityhub.dto.request.CreateSlaConfigurationRequest;
import com.openqualityhub.dto.response.SlaConfigurationResponse;
import com.openqualityhub.service.SlaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sla")
@RequiredArgsConstructor
@Tag(name = "SLA Management", description = "SLA configuration and tracking")
public class SlaController {

    private final SlaService slaService;

    @PostMapping
    @Operation(summary = "Create an SLA configuration")
    public ResponseEntity<SlaConfigurationResponse> create(@Valid @RequestBody CreateSlaConfigurationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(slaService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an SLA configuration")
    public ResponseEntity<SlaConfigurationResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateSlaConfigurationRequest request) {
        return ResponseEntity.ok(slaService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an SLA configuration by ID")
    public ResponseEntity<SlaConfigurationResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(slaService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List SLA configurations")
    public ResponseEntity<Page<SlaConfigurationResponse>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(slaService.getAll(pageable));
    }

    @GetMapping("/active")
    @Operation(summary = "List active SLA configurations")
    public ResponseEntity<List<SlaConfigurationResponse>> getActive() {
        return ResponseEntity.ok(slaService.getActive());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete an SLA configuration")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        slaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
