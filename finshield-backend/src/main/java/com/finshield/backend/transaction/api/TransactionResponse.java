package com.finshield.backend.transaction.api;

import com.finshield.backend.transaction.domain.RiskBand;
import com.finshield.backend.transaction.domain.Transaction;
import com.finshield.backend.transaction.domain.TransactionChannel;
import com.finshield.backend.transaction.domain.TransactionDecision;
import com.finshield.backend.transaction.domain.TransactionStatus;
import com.finshield.backend.transaction.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String transactionReference,
        UUID sourceAccountId,
        String customerNumber,
        String customerName,
        String maskedSourceAccountNumber,
        String maskedDestinationAccountNumber,
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
        BigDecimal riskScore,
        RiskBand riskBand,
        TransactionDecision decision,
        Instant createdAt,
        Instant updatedAt
) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getTransactionReference(),
                transaction.getSourceAccount().getId(),
                transaction.getSourceAccount().getCustomer().getCustomerNumber(),
                transaction.getSourceAccount().getCustomer().getFullName(),
                mask(transaction.getSourceAccount().getAccountNumber()),
                mask(transaction.getDestinationAccountNumber()),
                transaction.getBeneficiaryName(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getTransactionType(),
                transaction.getChannel(),
                transaction.getStatus(),
                transaction.getDeviceId(),
                transaction.getIpAddress(),
                transaction.getGeoLocation(),
                transaction.getInitiatedAt(),
                transaction.getRiskScore(),
                transaction.getRiskBand(),
                transaction.getDecision(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }

    private static String mask(String accountNumber) {
        return accountNumber.length() <= 4
                ? "****"
                : "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
