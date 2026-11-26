package com.openqualityhub.controller;

import com.openqualityhub.dto.request.CreateQaServiceRequest;
import com.openqualityhub.dto.response.QaServiceResponse;
import com.openqualityhub.service.QaServiceCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/qa-services")
@RequiredArgsConstructor
@Tag(name = "QA Service Catalog", description = "QA service definitions and catalog management")
public class QaServiceCatalogController {

    private final QaServiceCatalogService qaServiceCatalogService;

    @PostMapping
    @PreAuthorize("hasAuthority('QA_SERVICE_WRITE')")
    @Operation(summary = "Create a QA service")
    public ResponseEntity<QaServiceResponse> create(@Valid @RequestBody CreateQaServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(qaServiceCatalogService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('QA_SERVICE_WRITE')")
    @Operation(summary = "Update a QA service")
    public ResponseEntity<QaServiceResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateQaServiceRequest request) {
        return ResponseEntity.ok(qaServiceCatalogService.update(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('QA_SERVICE_READ')")
    @Operation(summary = "Get a QA service by ID")
    public ResponseEntity<QaServiceResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(qaServiceCatalogService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('QA_SERVICE_READ')")
    @Operation(summary = "List QA services")
    public ResponseEntity<Page<QaServiceResponse>> getAll(
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20) Pageable pageable) {
        if (category != null) {
            return ResponseEntity.ok(qaServiceCatalogService.getByCategory(category, pageable));
        }
        return ResponseEntity.ok(qaServiceCatalogService.getAll(pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('QA_SERVICE_WRITE')")
    @Operation(summary = "Soft-delete a QA service")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        qaServiceCatalogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
