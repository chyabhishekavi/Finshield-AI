package com.finshield.backend.audit;

import com.finshield.backend.audit.api.AuditLogResponse;
import com.finshield.backend.audit.domain.AuditAction;
import com.finshield.backend.audit.repository.AuditLogRepository;
import com.finshield.backend.common.api.ApiResponse;
import com.finshield.backend.common.api.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER')")
public class AuditLogController {
    private final AuditLogRepository repository;
    public AuditLogController(AuditLogRepository repository) { this.repository = repository; }

    @GetMapping
    public ApiResponse<PageResponse<AuditLogResponse>> list(
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) @Size(max = 100) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        String type = entityType == null || entityType.isBlank() ? null : entityType.trim();
        return ApiResponse.success(PageResponse.from(repository.search(actorUserId, action, type, entityId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))), AuditLogResponse::from));
    }
}
