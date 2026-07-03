package com.finshield.backend.casework.api;

import com.finshield.backend.casework.domain.CaseEvidence;
import com.finshield.backend.casework.domain.EvidenceType;
import java.time.Instant;
import java.util.UUID;

public record CaseEvidenceResponse(UUID id, UUID caseId, EvidenceType evidenceType,
        String fileName, String fileUrl, String description,
        UUID uploadedById, String uploadedByName, Instant uploadedAt) {
    public static CaseEvidenceResponse from(CaseEvidence value) {
        return new CaseEvidenceResponse(value.getId(), value.getInvestigationCase().getId(),
                value.getEvidenceType(), value.getFileName(), value.getFileUrl(), value.getDescription(),
                value.getUploadedBy().getId(), value.getUploadedBy().getFullName(), value.getUploadedAt());
    }
}
