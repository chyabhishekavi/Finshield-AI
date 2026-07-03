package com.finshield.backend.casework.api;

import com.finshield.backend.casework.domain.InvestigationCaseStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateCaseStatusRequest(@NotNull InvestigationCaseStatus status) {}
