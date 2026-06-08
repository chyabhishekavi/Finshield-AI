package com.finshield.backend.risk.ml;

import com.finshield.backend.transaction.domain.TransactionChannel;
import com.finshield.backend.transaction.domain.TransactionType;

import java.math.BigDecimal;

public record MlFraudPredictionRequest(
        String transactionReference,
        BigDecimal amount,
        BigDecimal averageCustomerAmount,
        String currency,
        TransactionType transactionType,
        TransactionChannel channel,
        int hourOfDay,
        long accountAgeDays,
        long transactionCount5m,
        BigDecimal totalAmount1h,
        int failedLoginCount,
        Long beneficiaryAgeHours,
        BigDecimal beneficiaryRiskScore,
        boolean deviceTrusted,
        Long deviceAgeDays,
        boolean ipAddressChanged,
        double geoDistanceKm,
        BigDecimal customerRiskScore,
        BigDecimal amlRiskScore
) {
}
