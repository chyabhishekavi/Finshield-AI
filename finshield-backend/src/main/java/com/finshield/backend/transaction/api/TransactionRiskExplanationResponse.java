package com.finshield.backend.transaction.api;

import com.finshield.backend.risk.domain.TransactionRiskScore;
import com.finshield.backend.risk.domain.TransactionRuleMatch;
import com.finshield.backend.transaction.domain.RiskBand;
import com.finshield.backend.transaction.domain.TransactionDecision;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TransactionRiskExplanationResponse(
        UUID transactionId,
        BigDecimal ruleScore,
        BigDecimal mlScore,
        BigDecimal customerRiskScore,
        BigDecimal deviceRiskScore,
        BigDecimal amlScore,
        BigDecimal finalScore,
        RiskBand riskBand,
        TransactionDecision decision,
        String explanationSummary,
        boolean mlFallbackUsed,
        Instant scoredAt,
        List<RuleMatchResponse> matchedRules
) {
    public TransactionRiskExplanationResponse { matchedRules = List.copyOf(matchedRules); }

    public static TransactionRiskExplanationResponse from(
            TransactionRiskScore score,
            List<TransactionRuleMatch> matches
    ) {
        return new TransactionRiskExplanationResponse(score.getTransaction().getId(), score.getRuleScore(),
                score.getMlScore(), score.getCustomerRiskScore(), score.getDeviceRiskScore(),
                score.getAmlScore(), score.getFinalScore(), score.getRiskBand(), score.getDecision(),
                score.getExplanationSummary(), score.isMlFallbackUsed(), score.getScoredAt(),
                matches.stream().map(RuleMatchResponse::from).toList());
    }

    public record RuleMatchResponse(String ruleCode, String ruleName,
            BigDecimal scoreImpact, String severity, String reason) {
        static RuleMatchResponse from(TransactionRuleMatch value) {
            return new RuleMatchResponse(value.getRuleCode(), value.getRuleName(), value.getScoreImpact(),
                    value.getSeverity().name(), value.getReason());
        }
    }
}
