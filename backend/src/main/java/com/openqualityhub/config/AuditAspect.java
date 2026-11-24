package com.openqualityhub.config;

import com.openqualityhub.entity.AuditEvent;
import com.openqualityhub.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @AfterReturning(
            pointcut = "execution(* com.openqualityhub.service.impl..*(..)) && @annotation(org.springframework.transaction.annotation.Transactional)",
            returning = "result"
    )
    public void auditServiceMethod(JoinPoint joinPoint, Object result) {
        try {
            String methodName = joinPoint.getSignature().getName();
            AuditEvent.AuditAction action = resolveAction(methodName);

            if (action == null) {
                return;
            }

            String entityType = resolveEntityType(joinPoint.getTarget().getClass().getSimpleName());
            UUID entityId = extractEntityId(result);

            String details = String.format("{\"method\":\"%s\"}", methodName);

            auditService.log(entityType, entityId, action, details);
        } catch (Exception e) {
            log.debug("Audit aspect failed (non-blocking): {}", e.getMessage());
        }
    }

    private AuditEvent.AuditAction resolveAction(String methodName) {
        String lower = methodName.toLowerCase();
        if (lower.startsWith("create") || lower.startsWith("add") || lower.startsWith("register")) {
            return AuditEvent.AuditAction.CREATE;
        }
        if (lower.startsWith("update") || lower.startsWith("edit") || lower.startsWith("modify")
                || lower.startsWith("toggle") || lower.startsWith("assign") || lower.startsWith("transition")) {
            return AuditEvent.AuditAction.UPDATE;
        }
        if (lower.startsWith("delete") || lower.startsWith("remove") || lower.startsWith("soft")) {
            return AuditEvent.AuditAction.DELETE;
        }
        if (lower.startsWith("import") || lower.startsWith("ingest")) {
            return AuditEvent.AuditAction.IMPORT;
        }
        if (lower.startsWith("evaluate") || lower.startsWith("generate")) {
            return AuditEvent.AuditAction.EVALUATE;
        }
        if (lower.startsWith("override")) {
            return AuditEvent.AuditAction.OVERRIDE;
        }
        if (lower.startsWith("export")) {
            return AuditEvent.AuditAction.EXPORT;
        }
        if (lower.startsWith("signoff") || lower.startsWith("sign") || lower.startsWith("approve") || lower.startsWith("reject")) {
            return AuditEvent.AuditAction.UPDATE;
        }
        return null;
    }

    private String resolveEntityType(String className) {
        return className
                .replace("ServiceImpl", "")
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toUpperCase();
    }

    private UUID extractEntityId(Object result) {
        if (result == null) {
            return null;
        }
        try {
            Method getId = result.getClass().getMethod("getId");
            Object id = getId.invoke(result);
            if (id instanceof UUID uuid) {
                return uuid;
            }
            if (id instanceof String s) {
                return UUID.fromString(s);
            }
        } catch (NoSuchMethodException e) {
            // Result doesn't have getId(), that's fine
        } catch (Exception e) {
            log.debug("Could not extract entity ID from result: {}", e.getMessage());
        }
        return null;
    }
}
