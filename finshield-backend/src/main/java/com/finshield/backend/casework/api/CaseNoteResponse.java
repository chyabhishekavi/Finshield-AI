package com.finshield.backend.casework.api;

import com.finshield.backend.casework.domain.CaseNote;
import java.time.Instant;
import java.util.UUID;

public record CaseNoteResponse(UUID id, UUID caseId, String noteText, UUID createdById,
        String createdByName, Instant createdAt, boolean internalOnly) {
    public static CaseNoteResponse from(CaseNote value) {
        return new CaseNoteResponse(value.getId(), value.getInvestigationCase().getId(), value.getNoteText(),
                value.getCreatedBy().getId(), value.getCreatedBy().getFullName(),
                value.getCreatedAt(), value.isInternalOnly());
    }
}
