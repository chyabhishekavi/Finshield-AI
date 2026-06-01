package com.finshield.backend.transaction.event;

import com.finshield.backend.transaction.domain.RiskBand;
import com.finshield.backend.transaction.domain.TransactionDecision;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionRiskScoredEvent(
        UUID eventId,
        UUID causationEventId,
        int schemaVersion,
        UUID transactionId,
        String transactionReference,
        BigDecimal riskScore,
        RiskBand riskBand,
        TransactionDecision decision,
        String scoringVersion,
        Instant scoredAt
) {
}
