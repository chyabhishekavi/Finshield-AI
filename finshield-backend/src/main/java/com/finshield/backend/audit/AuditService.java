package com.finshield.backend.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finshield.backend.audit.domain.AuditAction;
import com.finshield.backend.audit.domain.AuditLog;
import com.finshield.backend.audit.repository.AuditLogRepository;
import com.finshield.backend.auth.security.FinshieldUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.UUID;

@Service
public class AuditService {
    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void log(AuditAction action, String entityType, UUID entityId, Object oldValue, Object newValue) {
        logAs(currentActor(), action, entityType, entityId, oldValue, newValue);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void logAs(UUID actorId, AuditAction action, String entityType, UUID entityId,
            Object oldValue, Object newValue) {
        repository.save(new AuditLog(actorId, action, entityType, entityId,
                json(oldValue), json(newValue), requestIpAddress()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logIndependent(UUID actorId, AuditAction action, String entityType, UUID entityId,
            Object oldValue, Object newValue) {
        repository.save(new AuditLog(actorId, action, entityType, entityId,
                json(oldValue), json(newValue), requestIpAddress()));
    }

    private UUID currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof FinshieldUserPrincipal principal
                ? principal.getId() : null;
    }

    private String requestIpAddress() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) return null;
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        String value = forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr() : forwarded.split(",", 2)[0].trim();
        return value.length() <= 45 ? value : value.substring(0, 45);
    }

    private String json(Object value) {
        if (value == null) return null;
        try {
            String result = objectMapper.writeValueAsString(value);
            return result.length() <= 8000 ? result : result.substring(0, 8000);
        } catch (JsonProcessingException exception) {
            return "\"[unserializable audit value]\"";
        }
    }
}
