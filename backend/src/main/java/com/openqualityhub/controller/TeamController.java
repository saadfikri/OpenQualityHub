package com.openqualityhub.controller;

import com.openqualityhub.dto.request.CreateTeamRequest;
import com.openqualityhub.dto.request.UpdateTeamRequest;
import com.openqualityhub.dto.response.TeamResponse;
import com.openqualityhub.service.TeamService;
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
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
@Tag(name = "Teams", description = "Team management operations")
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    @PreAuthorize("hasAuthority('TEAM_WRITE')")
    @Operation(summary = "Create a new team")
    public ResponseEntity<TeamResponse> create(@Valid @RequestBody CreateTeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TEAM_READ')")
    @Operation(summary = "Get a team by ID")
    public ResponseEntity<TeamResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(teamService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TEAM_READ')")
    @Operation(summary = "Get all teams with pagination")
    public ResponseEntity<Page<TeamResponse>> getAll(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(teamService.search(search, pageable));
        }
        return ResponseEntity.ok(teamService.getAll(pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TEAM_WRITE')")
    @Operation(summary = "Update a team")
    public ResponseEntity<TeamResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateTeamRequest request) {
        return ResponseEntity.ok(teamService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TEAM_DELETE')")
    @Operation(summary = "Delete a team")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        teamService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
