package com.finshield.backend.customer.api;

import com.finshield.backend.customer.domain.AnnualIncomeRange;
import com.finshield.backend.customer.domain.CustomerRiskLevel;
import com.finshield.backend.customer.domain.KycStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateCustomerRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{4,30}") String customerNumber,
        @NotBlank @Size(min = 2, max = 150) String fullName,
        @NotNull @Past LocalDate dateOfBirth,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Pattern(regexp = "\\+?[0-9]{7,20}") String phone,
        @NotNull KycStatus kycStatus,
        @NotNull CustomerRiskLevel customerRiskLevel,
        @NotBlank @Size(max = 120) String occupation,
        @NotNull AnnualIncomeRange annualIncomeRange,
        @NotBlank @Size(min = 2, max = 100) String country,
        @NotBlank @Size(min = 2, max = 100) String city,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{2,20}") String branchCode
) {
}
