package com.finshield.backend.customer.api;

import com.finshield.backend.customer.domain.Customer;
import com.finshield.backend.customer.domain.CustomerRiskLevel;
import com.finshield.backend.customer.domain.KycStatus;

import java.util.UUID;

public record CustomerSummaryResponse(
        UUID id,
        String customerNumber,
        String fullName,
        KycStatus kycStatus,
        CustomerRiskLevel customerRiskLevel,
        String country,
        String city,
        String branchCode
) {

    public static CustomerSummaryResponse from(Customer customer) {
        return new CustomerSummaryResponse(
                customer.getId(),
                customer.getCustomerNumber(),
                customer.getFullName(),
                customer.getKycStatus(),
                customer.getCustomerRiskLevel(),
                customer.getCountry(),
                customer.getCity(),
                customer.getBranchCode()
        );
    }
}
