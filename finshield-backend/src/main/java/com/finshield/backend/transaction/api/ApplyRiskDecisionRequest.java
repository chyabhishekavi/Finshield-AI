package com.finshield.backend.transaction.api;

import com.finshield.backend.transaction.domain.RiskBand;
import com.finshield.backend.transaction.domain.TransactionDecision;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ApplyRiskDecisionRequest(
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00")
        @Digits(integer = 3, fraction = 2) BigDecimal riskScore,
        @NotNull RiskBand riskBand,
        @NotNull TransactionDecision decision
) {
}
