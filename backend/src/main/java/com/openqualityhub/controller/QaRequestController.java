package com.openqualityhub.controller;

import com.openqualityhub.dto.request.AddCommentRequest;
import com.openqualityhub.dto.request.CreateQaServiceRequestRequest;
import com.openqualityhub.dto.request.OverridePriorityRequest;
import com.openqualityhub.dto.request.UpdateQaServiceRequestRequest;
import com.openqualityhub.dto.response.QaRequestDashboardResponse;
import com.openqualityhub.dto.response.QaServiceRequestResponse;
import com.openqualityhub.security.UserPrincipal;
import com.openqualityhub.service.QaRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/qa-requests")
@RequiredArgsConstructor
@Tag(name = "QA Service Requests", description = "Cross-team QA service request management")
public class QaRequestController {

    private final QaRequestService qaRequestService;

    @PostMapping
    @Operation(summary = "Create a QA service request")
    public ResponseEntity<QaServiceRequestResponse> create(
            @Valid @RequestBody CreateQaServiceRequestRequest request,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(qaRequestService.create(request, userId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a QA service request")
    public ResponseEntity<QaServiceRequestResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQaServiceRequestRequest request,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ResponseEntity.ok(qaRequestService.update(id, request, userId));
    }

    @PatchMapping("/{id}/transition")
    @Operation(summary = "Transition request to a new status")
    public ResponseEntity<QaServiceRequestResponse> transition(
            @PathVariable UUID id,
            @RequestParam String status,
            @RequestParam(required = false) String comment,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ResponseEntity.ok(qaRequestService.transition(id, status, userId, comment));
    }

    @PatchMapping("/{id}/priority-override")
    @Operation(summary = "Override request priority (QA Manager only)")
    public ResponseEntity<QaServiceRequestResponse> overridePriority(
            @PathVariable UUID id,
            @Valid @RequestBody OverridePriorityRequest request,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ResponseEntity.ok(qaRequestService.overridePriority(id, request, userId));
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add a comment to a request")
    public ResponseEntity<QaServiceRequestResponse> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody AddCommentRequest request,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ResponseEntity.ok(qaRequestService.addComment(id, request.getComment(), userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a QA service request by ID")
    public ResponseEntity<QaServiceRequestResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(qaRequestService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List QA service requests")
    public ResponseEntity<Page<QaServiceRequestResponse>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID teamId,
            @RequestParam(required = false) UUID assigneeId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        if (status != null) {
            return ResponseEntity.ok(qaRequestService.getByStatus(status, pageable));
        }
        if (teamId != null) {
            return ResponseEntity.ok(qaRequestService.getByTeam(teamId, pageable));
        }
        if (assigneeId != null) {
            return ResponseEntity.ok(qaRequestService.getByAssignee(assigneeId, pageable));
        }
        return ResponseEntity.ok(qaRequestService.getAll(pageable));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get QA request dashboard statistics")
    public ResponseEntity<QaRequestDashboardResponse> getDashboard() {
        return ResponseEntity.ok(qaRequestService.getDashboard());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a QA service request")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        qaRequestService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private UUID extractUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal up) {
            return up.getId();
        }
        return null;
    }
}
