package com.finshield.backend.casework.repository;

import com.finshield.backend.casework.domain.CaseEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CaseEvidenceRepository extends JpaRepository<CaseEvidence, UUID> {
    List<CaseEvidence> findAllByInvestigationCaseIdOrderByUploadedAtAsc(UUID caseId);
}
