package com.finshield.backend.casework.repository;

import com.finshield.backend.casework.domain.CaseStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CaseStatusHistoryRepository extends JpaRepository<CaseStatusHistory, UUID> {
    List<CaseStatusHistory> findAllByInvestigationCaseIdOrderByCreatedAtAsc(UUID caseId);
}
