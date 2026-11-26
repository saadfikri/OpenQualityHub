package com.openqualityhub.controller;

import com.openqualityhub.dto.request.CreateDefectRequest;
import com.openqualityhub.dto.request.UpdateDefectRequest;
import com.openqualityhub.dto.response.DefectResponse;
import com.openqualityhub.security.JwtService;
import com.openqualityhub.service.DefectService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/defects")
@RequiredArgsConstructor
@Tag(name = "Defects", description = "Defect management")
public class DefectController {

    private final DefectService defectService;

    @PostMapping
    @PreAuthorize("hasAuthority('DEFECT_WRITE')")
    @Operation(summary = "Create a defect")
    public ResponseEntity<DefectResponse> create(
            @Valid @RequestBody CreateDefectRequest request,
            Authentication authentication) {
        UUID reportedById = null;
        if (authentication != null && authentication.getPrincipal() instanceof com.openqualityhub.security.UserPrincipal up) {
            reportedById = up.getId();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(defectService.create(request, reportedById));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DEFECT_WRITE')")
    @Operation(summary = "Update a defect")
    public ResponseEntity<DefectResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDefectRequest request) {
        return ResponseEntity.ok(defectService.update(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DEFECT_READ')")
    @Operation(summary = "Get a defect by ID")
    public ResponseEntity<DefectResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(defectService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DEFECT_READ')")
    @Operation(summary = "List defects with pagination")
    public ResponseEntity<Page<DefectResponse>> getAll(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        if (projectId != null) {
            return ResponseEntity.ok(defectService.getByProjectId(projectId, pageable));
        }
        if (severity != null) {
            return ResponseEntity.ok(defectService.getBySeverity(severity, pageable));
        }
        if (status != null) {
            return ResponseEntity.ok(defectService.getByStatus(status, pageable));
        }
        return ResponseEntity.ok(defectService.getAll(pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DEFECT_DELETE')")
    @Operation(summary = "Soft-delete a defect")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        defectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
