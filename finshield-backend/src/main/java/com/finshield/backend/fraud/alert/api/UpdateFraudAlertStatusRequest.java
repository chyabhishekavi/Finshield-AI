package com.finshield.backend.fraud.alert.api;

import com.finshield.backend.fraud.alert.domain.FraudAlertStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateFraudAlertStatusRequest(
        @NotNull FraudAlertStatus status
) {
}
