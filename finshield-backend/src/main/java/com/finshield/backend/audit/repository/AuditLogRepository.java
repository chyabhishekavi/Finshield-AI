package com.finshield.backend.audit.repository;

import com.finshield.backend.audit.domain.AuditAction;
import com.finshield.backend.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    @Query("""
            select a from AuditLog a
            where (:actorId is null or a.actorUserId = :actorId)
              and (:action is null or a.action = :action)
              and (:entityType is null or a.entityType = :entityType)
              and (:entityId is null or a.entityId = :entityId)
            """)
    Page<AuditLog> search(@Param("actorId") UUID actorId, @Param("action") AuditAction action,
            @Param("entityType") String entityType, @Param("entityId") UUID entityId, Pageable pageable);
}
