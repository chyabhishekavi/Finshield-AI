package com.finshield.backend.fraud.alert.api;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record EscalateFraudAlertRequest(
        @NotBlank @Size(max = 1000) String reason,
        @Future Instant dueAt
) {
}
