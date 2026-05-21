package com.finshield.backend.beneficiary.api;

import com.finshield.backend.beneficiary.domain.Beneficiary;
import com.finshield.backend.beneficiary.domain.BeneficiaryStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BeneficiaryResponse(
        UUID id,
        UUID customerId,
        String beneficiaryName,
        String maskedAccountNumber,
        String bankName,
        String ifscCode,
        Instant addedAt,
        BigDecimal riskScore,
        BeneficiaryStatus status,
        Instant updatedAt
) {

    public static BeneficiaryResponse from(Beneficiary beneficiary) {
        return new BeneficiaryResponse(
                beneficiary.getId(),
                beneficiary.getCustomer().getId(),
                beneficiary.getBeneficiaryName(),
                maskAccountNumber(beneficiary.getBeneficiaryAccountNumber()),
                beneficiary.getBankName(),
                beneficiary.getIfscCode(),
                beneficiary.getAddedAt(),
                beneficiary.getRiskScore(),
                beneficiary.getStatus(),
                beneficiary.getUpdatedAt()
        );
    }

    private static String maskAccountNumber(String accountNumber) {
        if (accountNumber.length() <= 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
