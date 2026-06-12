package com.finshield.backend.fraud.api;

import com.finshield.backend.fraud.domain.FraudRuleType;
import com.finshield.backend.fraud.domain.RuleOperator;
import com.finshield.backend.fraud.domain.RuleSeverity;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateFraudRuleRequest(
        @NotBlank @Size(min = 3, max = 150) String ruleName,
        @NotBlank @Size(max = 1000) String description,
        @NotNull FraudRuleType ruleType,
        @NotBlank @Size(max = 150) String conditionField,
        @NotNull RuleOperator operator,
        @NotBlank @Size(max = 500) String thresholdValue,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00")
        @Digits(integer = 3, fraction = 2) BigDecimal scoreImpact,
        @NotNull Boolean active,
        @NotNull RuleSeverity severity
) {
}
