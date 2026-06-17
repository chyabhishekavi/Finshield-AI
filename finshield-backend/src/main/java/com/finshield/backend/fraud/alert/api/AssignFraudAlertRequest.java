package com.finshield.backend.fraud.alert.api;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record AssignFraudAlertRequest(
        @NotNull UUID assignedTo,
        @Future Instant dueAt
) {
}
