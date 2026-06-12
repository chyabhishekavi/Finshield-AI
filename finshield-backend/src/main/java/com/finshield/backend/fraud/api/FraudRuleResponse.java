package com.finshield.backend.fraud.api;

import com.finshield.backend.fraud.domain.FraudRule;
import com.finshield.backend.fraud.domain.FraudRuleType;
import com.finshield.backend.fraud.domain.RuleOperator;
import com.finshield.backend.fraud.domain.RuleSeverity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FraudRuleResponse(
        UUID id,
        String ruleCode,
        String ruleName,
        String description,
        FraudRuleType ruleType,
        String conditionField,
        RuleOperator operator,
        String thresholdValue,
        BigDecimal scoreImpact,
        boolean active,
        RuleSeverity severity,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {

    public static FraudRuleResponse from(FraudRule rule) {
        return new FraudRuleResponse(
                rule.getId(),
                rule.getRuleCode(),
                rule.getRuleName(),
                rule.getDescription(),
                rule.getRuleType(),
                rule.getConditionField(),
                rule.getOperator(),
                rule.getThresholdValue(),
                rule.getScoreImpact(),
                rule.isActive(),
                rule.getSeverity(),
                rule.getCreatedBy(),
                rule.getUpdatedBy(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }
}
