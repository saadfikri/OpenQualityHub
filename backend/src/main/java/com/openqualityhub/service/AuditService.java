package com.openqualityhub.service;

import com.openqualityhub.dto.response.AuditEventResponse;
import com.openqualityhub.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AuditService {

    void log(String entityType, UUID entityId, AuditEvent.AuditAction action, String details);

    Page<AuditEventResponse> getEvents(String entityType, AuditEvent.AuditAction action, UUID userId, Pageable pageable);
}
