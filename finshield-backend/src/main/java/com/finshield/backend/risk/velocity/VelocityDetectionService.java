package com.finshield.backend.risk.velocity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface VelocityDetectionService {

    VelocityMetrics recordTransaction(
            UUID eventId,
            UUID customerId,
            String beneficiaryAccountNumber,
            BigDecimal amount,
            String deviceId,
            Instant occurredAt
    );

    void recordFailedOrSuspiciousAttempt(
            UUID eventId,
            String deviceId,
            Instant occurredAt
    );
}
