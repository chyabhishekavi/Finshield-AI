package com.finshield.backend.fraud.alert.api;

import com.finshield.backend.fraud.alert.domain.FraudAlert;
import com.finshield.backend.fraud.alert.domain.FraudAlertSeverity;
import com.finshield.backend.fraud.alert.domain.FraudAlertStatus;
import com.finshield.backend.transaction.domain.RiskBand;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FraudAlertResponse(
        UUID id,
        String alertNumber,
        UUID transactionId,
        String transactionReference,
        UUID customerId,
        String customerNumber,
        String customerName,
        BigDecimal riskScore,
        RiskBand riskBand,
        FraudAlertSeverity severity,
        FraudAlertStatus status,
        UUID assignedToId,
        String assignedToName,
        Instant createdAt,
        Instant updatedAt,
        Instant dueAt,
        boolean overdue,
        String alertSummary,
        long version
) {
    public static FraudAlertResponse from(FraudAlert alert, Instant now) {
        return new FraudAlertResponse(
                alert.getId(),
                alert.getAlertNumber(),
                alert.getTransaction().getId(),
                alert.getTransaction().getTransactionReference(),
                alert.getCustomer().getId(),
                alert.getCustomer().getCustomerNumber(),
                alert.getCustomer().getFullName(),
                alert.getRiskScore(),
                alert.getRiskBand(),
                alert.getSeverity(),
                alert.getStatus(),
                alert.getAssignedTo() == null ? null : alert.getAssignedTo().getId(),
                alert.getAssignedTo() == null ? null : alert.getAssignedTo().getFullName(),
                alert.getCreatedAt(),
                alert.getUpdatedAt(),
                alert.getDueAt(),
                !alert.getStatus().isTerminal() && alert.getDueAt().isBefore(now),
                alert.getAlertSummary(),
                alert.getVersion()
        );
    }
}
