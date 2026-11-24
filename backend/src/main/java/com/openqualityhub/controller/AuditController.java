package com.openqualityhub.controller;

import com.openqualityhub.dto.response.AuditEventResponse;
import com.openqualityhub.entity.AuditEvent;
import com.openqualityhub.service.AuditService;
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
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Trail", description = "Audit event log and compliance trail")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "List audit events with optional filters")
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    public ResponseEntity<Page<AuditEventResponse>> getEvents(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) AuditEvent.AuditAction action,
            @RequestParam(required = false) UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(auditService.getEvents(entityType, action, userId, pageable));
    }
}
