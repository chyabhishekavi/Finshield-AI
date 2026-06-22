package com.finshield.backend.fraud.alert.event;

import com.finshield.backend.fraud.alert.domain.FraudAlertSeverity;
import com.finshield.backend.fraud.alert.domain.FraudAlertStatus;
import com.finshield.backend.transaction.domain.RiskBand;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FraudAlertCreatedEvent(UUID eventId, UUID alertId, String alertNumber,
        UUID transactionId, String transactionReference, UUID customerId,
        BigDecimal riskScore, RiskBand riskBand, FraudAlertSeverity severity,
        FraudAlertStatus status, Instant createdAt) {}
