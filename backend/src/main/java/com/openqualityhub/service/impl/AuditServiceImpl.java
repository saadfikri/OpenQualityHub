package com.openqualityhub.service.impl;

import com.openqualityhub.dto.response.AuditEventResponse;
import com.openqualityhub.entity.AuditEvent;
import com.openqualityhub.entity.Organization;
import com.openqualityhub.entity.User;
import com.openqualityhub.repository.AuditEventRepository;
import com.openqualityhub.repository.OrganizationRepository;
import com.openqualityhub.repository.UserRepository;
import com.openqualityhub.security.UserPrincipal;
import com.openqualityhub.service.AuditService;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditServiceImpl implements AuditService {

    private static final UUID DEFAULT_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final AuditEventRepository auditEventRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void log(String entityType, UUID entityId, AuditEvent.AuditAction action, String details) {
        try {
            Organization org = organizationRepository.findById(DEFAULT_ORG_ID).orElse(null);
            if (org == null) {
                log.warn("Default organization not found, skipping audit log");
                return;
            }

            User user = resolveCurrentUser();
            String ipAddress = resolveIpAddress();
            String userAgent = resolveUserAgent();

            AuditEvent event = AuditEvent.builder()
                    .organization(org)
                    .user(user)
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .details(details != null ? details : "{}")
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            auditEventRepository.save(event);
            log.debug("Audit logged: {} {} {} {}", action, entityType, entityId, user != null ? user.getEmail() : "anonymous");
        } catch (Exception e) {
            log.error("Failed to write audit log: {}", e.getMessage(), e);
        }
    }

    @Override
    public Page<AuditEventResponse> getEvents(String entityType, AuditEvent.AuditAction action, UUID userId, Pageable pageable) {
        Specification<AuditEvent> spec = buildSpecification(entityType, action, userId);
        return auditEventRepository.findAll(spec, pageable).map(this::toResponse);
    }

    private Specification<AuditEvent> buildSpecification(String entityType, AuditEvent.AuditAction action, UUID userId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (entityType != null && !entityType.isBlank()) {
                predicates.add(cb.equal(root.get("entityType"), entityType));
            }
            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }

            query.orderBy(cb.desc(root.get("createdAt")));

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AuditEventResponse toResponse(AuditEvent event) {
        return AuditEventResponse.builder()
                .id(event.getId())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .action(event.getAction().name())
                .details(event.getDetails())
                .userName(event.getUser() != null ? event.getUser().getDisplayName() : null)
                .ipAddress(event.getIpAddress())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return userRepository.findById(principal.getId()).orElse(null);
        }
        return null;
    }

    private String resolveIpAddress() {
        HttpServletRequest request = getHttpRequest();
        if (request == null) return null;

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveUserAgent() {
        HttpServletRequest request = getHttpRequest();
        return request != null ? request.getHeader("User-Agent") : null;
    }

    private HttpServletRequest getHttpRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
