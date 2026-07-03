package com.finshield.backend.casework.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignCaseRequest(@NotNull UUID assignedTo) {}
