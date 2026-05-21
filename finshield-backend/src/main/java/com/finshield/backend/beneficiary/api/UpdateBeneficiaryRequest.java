package com.finshield.backend.beneficiary.api;

import com.finshield.backend.beneficiary.domain.BeneficiaryStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateBeneficiaryRequest(
        @Size(min = 2, max = 150) String beneficiaryName,
        @Size(min = 2, max = 150) String bankName,
        @Pattern(regexp = "[A-Za-z]{4}0[A-Za-z0-9]{6}") String ifscCode,
        @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2) BigDecimal riskScore,
        BeneficiaryStatus status
) {
}
