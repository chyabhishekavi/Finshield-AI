package com.finshield.backend.risk.velocity;

import java.math.BigDecimal;
import java.time.Instant;

public record VelocityMetrics(
        long transactionCount5Minutes,
        BigDecimal totalAmount1Hour,
        long distinctBeneficiaries24Hours,
        long failedOrSuspiciousDeviceAttempts24Hours,
        Instant measuredAt
) {
}
