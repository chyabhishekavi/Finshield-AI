package com.finshield.backend.transaction.event;

import com.finshield.backend.transaction.domain.TransactionChannel;
import com.finshield.backend.transaction.domain.TransactionStatus;
import com.finshield.backend.transaction.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionEvent(
        UUID eventId,
        int schemaVersion,
        UUID transactionId,
        String transactionReference,
        UUID sourceAccountId,
        String destinationAccountNumber,
        String beneficiaryName,
        BigDecimal amount,
        String currency,
        TransactionType transactionType,
        TransactionChannel channel,
        TransactionStatus status,
        String deviceId,
        String ipAddress,
        String geoLocation,
        Instant initiatedAt,
        Instant publishedAt
) {
}
