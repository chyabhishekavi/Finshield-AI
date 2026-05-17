package com.finshield.backend.customer.api;

import com.finshield.backend.customer.domain.CustomerRiskLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRiskRequest(
        @NotNull CustomerRiskLevel riskLevel,
        @NotBlank @Size(max = 500) String reason
) {
}
