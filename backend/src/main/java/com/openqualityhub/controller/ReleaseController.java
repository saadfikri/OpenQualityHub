package com.openqualityhub.controller;

import com.openqualityhub.dto.response.ReleaseResponse;
import com.openqualityhub.service.ReleaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/releases")
@RequiredArgsConstructor
@Tag(name = "Releases", description = "Release management")
public class ReleaseController {

    private final ReleaseService releaseService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RELEASE_READ')")
    @Operation(summary = "Get a release by ID")
    public ResponseEntity<ReleaseResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(releaseService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('RELEASE_READ')")
    @Operation(summary = "List releases with pagination")
    public ResponseEntity<Page<ReleaseResponse>> getAll(
            @RequestParam(required = false) UUID projectId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        if (projectId != null) {
            return ResponseEntity.ok(releaseService.getByProjectId(projectId, pageable));
        }
        return ResponseEntity.ok(releaseService.getAll(pageable));
    }
}
