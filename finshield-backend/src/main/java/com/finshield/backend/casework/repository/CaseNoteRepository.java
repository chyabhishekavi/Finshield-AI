package com.finshield.backend.casework.repository;

import com.finshield.backend.casework.domain.CaseNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CaseNoteRepository extends JpaRepository<CaseNote, UUID> {
    List<CaseNote> findAllByInvestigationCaseIdOrderByCreatedAtAsc(UUID caseId);
}
