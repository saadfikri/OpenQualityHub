package com.openqualityhub.controller;

import com.openqualityhub.dto.response.ContributorActivityResponse;
import com.openqualityhub.dto.response.ContributorDetailResponse;
import com.openqualityhub.dto.response.ContributorResponse;
import com.openqualityhub.service.ContributorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contributors")
@RequiredArgsConstructor
@Tag(name = "Contributors", description = "Contributor tracking and activity history")
public class ContributorController {

    private final ContributorService contributorService;

    @GetMapping
    @Operation(summary = "List all contributors")
    @PreAuthorize("hasAuthority('CONTRIBUTOR_READ')")
    public ResponseEntity<Page<ContributorResponse>> getAll(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "lastActivityAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(contributorService.search(search, pageable));
        }
        return ResponseEntity.ok(contributorService.getAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get contributor details with activity breakdown")
    @PreAuthorize("hasAuthority('CONTRIBUTOR_READ')")
    public ResponseEntity<ContributorDetailResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(contributorService.getById(id));
    }

    @GetMapping("/{id}/activities")
    @Operation(summary = "Get contributor activity timeline")
    @PreAuthorize("hasAuthority('CONTRIBUTOR_READ')")
    public ResponseEntity<Page<ContributorActivityResponse>> getActivities(
            @PathVariable UUID id,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(contributorService.getActivities(id, pageable));
    }

    @GetMapping("/by-project/{projectId}")
    @Operation(summary = "Get contributor activities for a project")
    @PreAuthorize("hasAuthority('CONTRIBUTOR_READ')")
    public ResponseEntity<Page<ContributorActivityResponse>> getByProject(
            @PathVariable UUID projectId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(contributorService.getActivitiesByProject(projectId, pageable));
    }

    @PostMapping("/{id}/link-user")
    @Operation(summary = "Link a contributor to an OQH user account")
    @PreAuthorize("hasAuthority('CONTRIBUTOR_WRITE')")
    public ResponseEntity<ContributorResponse> linkUser(
            @PathVariable UUID id,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(contributorService.linkUser(id, userId));
    }

    @DeleteMapping("/{id}/link-user")
    @Operation(summary = "Unlink a contributor from an OQH user account")
    @PreAuthorize("hasAuthority('CONTRIBUTOR_WRITE')")
    public ResponseEntity<ContributorResponse> unlinkUser(@PathVariable UUID id) {
        return ResponseEntity.ok(contributorService.unlinkUser(id));
    }
}
