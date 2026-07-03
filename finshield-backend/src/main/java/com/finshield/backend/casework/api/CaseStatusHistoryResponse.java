package com.finshield.backend.casework.api;

import com.finshield.backend.casework.domain.CaseStatusHistory;
import com.finshield.backend.casework.domain.InvestigationCaseStatus;
import java.time.Instant;
import java.util.UUID;

public record CaseStatusHistoryResponse(UUID id, InvestigationCaseStatus oldStatus,
        InvestigationCaseStatus newStatus, UUID changedByUserId, Instant createdAt) {
    public static CaseStatusHistoryResponse from(CaseStatusHistory value) {
        return new CaseStatusHistoryResponse(value.getId(), value.getOldStatus(), value.getNewStatus(),
                value.getChangedByUserId(), value.getCreatedAt());
    }
}
