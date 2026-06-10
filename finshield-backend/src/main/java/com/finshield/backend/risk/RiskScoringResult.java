package com.finshield.backend.risk;

import com.finshield.backend.transaction.domain.RiskBand;
import com.finshield.backend.transaction.domain.TransactionDecision;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RiskScoringResult(
        UUID riskScoreId,
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
        Instant scoredAt
) {
}
