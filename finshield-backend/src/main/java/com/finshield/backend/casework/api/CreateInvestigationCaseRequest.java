package com.finshield.backend.casework.api;

import com.finshield.backend.casework.domain.CasePriority;
import com.finshield.backend.casework.domain.InvestigationCaseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateInvestigationCaseRequest(
        @NotNull InvestigationCaseType caseType,
        @NotNull UUID primaryTransactionId,
        UUID linkedAlertId,
        @NotNull CasePriority priority,
        @NotBlank @Size(max = 4000) String summary
) {}
