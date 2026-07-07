package com.finshield.backend.audit.domain;

import com.finshield.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.UUID;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_actor_created", columnList = "actor_user_id, created_at"),
        @Index(name = "idx_audit_action_created", columnList = "action, created_at")
})
public class AuditLog extends BaseEntity {
    @Column(name = "actor_user_id", updatable = false) private UUID actorUserId;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 50)
    private AuditAction action;
    @NotBlank @Size(max = 100) @Column(name = "entity_type", nullable = false, updatable = false, length = 100)
    private String entityType;
    @Column(name = "entity_id", updatable = false) private UUID entityId;
    @Size(max = 8000) @Column(name = "old_value", updatable = false, length = 8000) private String oldValue;
    @Size(max = 8000) @Column(name = "new_value", updatable = false, length = 8000) private String newValue;
    @Size(max = 45) @Column(name = "ip_address", updatable = false, length = 45) private String ipAddress;

    protected AuditLog() {}
    public AuditLog(UUID actorUserId, AuditAction action, String entityType, UUID entityId,
            String oldValue, String newValue, String ipAddress) {
        this.actorUserId = actorUserId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.ipAddress = ipAddress;
    }
    public UUID getActorUserId() { return actorUserId; }
    public AuditAction getAction() { return action; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public String getIpAddress() { return ipAddress; }
}
