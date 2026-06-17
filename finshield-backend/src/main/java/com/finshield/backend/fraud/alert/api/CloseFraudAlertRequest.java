package com.finshield.backend.fraud.alert.api;

import com.finshield.backend.fraud.alert.domain.FraudAlertStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CloseFraudAlertRequest(
        @NotNull FraudAlertStatus status,
        @NotBlank @Size(max = 1000) String resolution
) {
}
