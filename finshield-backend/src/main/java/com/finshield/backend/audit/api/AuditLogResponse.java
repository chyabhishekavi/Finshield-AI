package com.finshield.backend.audit.api;

import com.finshield.backend.audit.domain.AuditAction;
import com.finshield.backend.audit.domain.AuditLog;
import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(UUID id, UUID actorUserId, AuditAction action,
        String entityType, UUID entityId, String oldValue, String newValue,
        String ipAddress, Instant createdAt) {
    public static AuditLogResponse from(AuditLog value) {
        return new AuditLogResponse(value.getId(), value.getActorUserId(), value.getAction(),
                value.getEntityType(), value.getEntityId(), value.getOldValue(), value.getNewValue(),
                value.getIpAddress(), value.getCreatedAt());
    }
}
