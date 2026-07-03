package com.finshield.backend.casework.api;

import com.finshield.backend.casework.domain.CaseDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecordCaseDecisionRequest(
        @NotNull CaseDecision decision,
        @NotBlank @Size(max = 2000) String rationale
) {}
