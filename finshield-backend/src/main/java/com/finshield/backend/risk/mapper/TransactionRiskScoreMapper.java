package com.finshield.backend.risk.mapper;

import com.finshield.backend.fraud.api.RuleEvaluationResult;
import com.finshield.backend.risk.domain.TransactionRiskScore;
import com.finshield.backend.risk.domain.TransactionRuleMatch;
import com.finshield.backend.transaction.domain.RiskBand;
import com.finshield.backend.transaction.domain.Transaction;
import com.finshield.backend.transaction.domain.TransactionDecision;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TransactionRiskScoreMapper {

    public TransactionRiskScore toRiskScore(
            UUID sourceEventId,
            Transaction transaction,
            RuleEvaluationResult ruleEvaluation,
            BigDecimal ruleScore,
            BigDecimal mlScore,
            BigDecimal customerRiskScore,
            BigDecimal deviceRiskScore,
            BigDecimal amlScore,
            BigDecimal finalScore,
            RiskBand riskBand,
            TransactionDecision decision,
            String scoringVersion,
            String mlModelVersion,
            boolean mlFallbackUsed,
            List<String> mlExplanations,
            List<String> amlExplanations,
            Instant scoredAt
    ) {
        return new TransactionRiskScore(
                sourceEventId,
                transaction,
                ruleScore,
                mlScore,
                customerRiskScore,
                deviceRiskScore,
                amlScore,
                finalScore,
                riskBand,
                decision,
                scoringVersion,
                mlModelVersion,
                mlFallbackUsed,
                explanationSummary(
                        ruleEvaluation,
                        ruleScore,
                        mlScore,
                        customerRiskScore,
                        deviceRiskScore,
                        amlScore,
                        finalScore,
                        riskBand,
                        decision,
                        mlFallbackUsed,
                        mlExplanations,
                        amlExplanations
                ),
                scoredAt
        );
    }

    public List<TransactionRuleMatch> toRuleMatches(
            TransactionRiskScore riskScore,
            Transaction transaction,
            RuleEvaluationResult evaluation
    ) {
        return evaluation.matchedRules().stream()
                .map(match -> new TransactionRuleMatch(
                        riskScore,
                        transaction,
                        match.ruleCode(),
                        match.ruleName(),
                        match.severity(),
                        match.scoreImpact(),
                        match.reason()
                ))
                .toList();
    }

    private String explanationSummary(
            RuleEvaluationResult evaluation,
            BigDecimal ruleScore,
            BigDecimal mlScore,
            BigDecimal customerRiskScore,
            BigDecimal deviceRiskScore,
            BigDecimal amlScore,
            BigDecimal finalScore,
            RiskBand riskBand,
            TransactionDecision decision,
            boolean mlFallbackUsed,
            List<String> mlExplanations,
            List<String> amlExplanations
    ) {
        String matchedCodes = evaluation.matchedRules().isEmpty()
                ? "none"
                : evaluation.matchedRules().stream()
                .map(RuleEvaluationResult.MatchedRule::ruleCode)
                .collect(Collectors.joining(", "));
        String configurationWarning = evaluation.evaluationErrors().isEmpty()
                ? ""
                : " Active rule configuration errors triggered fail-safe scoring.";
        String mlExplanation = mlExplanations == null || mlExplanations.isEmpty()
                ? ""
                : " ML explanation: " + String.join("; ", mlExplanations) + ".";
        String amlExplanation = amlExplanations == null || amlExplanations.isEmpty()
                ? ""
                : " AML explanation: " + String.join("; ", amlExplanations) + ".";

        String summary = String.format(
                Locale.ROOT,
                "Final score %s (%s), decision %s. Components: rules=%s, ML=%s%s, customer=%s, "
                        + "device=%s, AML=%s. Matched rules: %s.%s%s%s",
                finalScore.toPlainString(),
                riskBand,
                decision,
                ruleScore.toPlainString(),
                mlScore.toPlainString(),
                mlFallbackUsed ? " [fallback]" : "",
                customerRiskScore.toPlainString(),
                deviceRiskScore.toPlainString(),
                amlScore.toPlainString(),
                matchedCodes,
                configurationWarning,
                mlExplanation,
                amlExplanation
        );
        return summary.length() <= 2000 ? summary : summary.substring(0, 1997) + "...";
    }
}
