package com.finshield.backend.customer.api;

import com.finshield.backend.customer.domain.AnnualIncomeRange;
import com.finshield.backend.customer.domain.Customer;
import com.finshield.backend.customer.domain.CustomerRiskLevel;
import com.finshield.backend.customer.domain.KycStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String customerNumber,
        String fullName,
        LocalDate dateOfBirth,
        String email,
        String phone,
        KycStatus kycStatus,
        CustomerRiskLevel customerRiskLevel,
        String occupation,
        AnnualIncomeRange annualIncomeRange,
        String country,
        String city,
        String branchCode,
        Instant createdAt,
        Instant updatedAt
) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getCustomerNumber(),
                customer.getFullName(),
                customer.getDateOfBirth(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getKycStatus(),
                customer.getCustomerRiskLevel(),
                customer.getOccupation(),
                customer.getAnnualIncomeRange(),
                customer.getCountry(),
                customer.getCity(),
                customer.getBranchCode(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}
