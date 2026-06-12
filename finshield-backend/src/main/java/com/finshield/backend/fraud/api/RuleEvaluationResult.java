package com.finshield.backend.fraud.api;

import com.finshield.backend.fraud.domain.FraudRuleType;
import com.finshield.backend.fraud.domain.RuleSeverity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RuleEvaluationResult(
        List<MatchedRule> matchedRules,
        List<String> reasons,
        BigDecimal totalRuleScore,
        int evaluatedRuleCount,
        List<RuleEvaluationError> evaluationErrors,
        Instant evaluatedAt
) {

    public RuleEvaluationResult {
        matchedRules = List.copyOf(matchedRules);
        reasons = List.copyOf(reasons);
        evaluationErrors = List.copyOf(evaluationErrors);
    }

    public record MatchedRule(
            UUID ruleId,
            String ruleCode,
            String ruleName,
            FraudRuleType ruleType,
            RuleSeverity severity,
            BigDecimal scoreImpact,
            String reason
    ) {
    }

    public record RuleEvaluationError(String ruleCode, String message) {
    }
}
