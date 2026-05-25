package com.finshield.backend.transaction.api;

import com.finshield.backend.device.validation.ValidIpAddress;
import com.finshield.backend.transaction.domain.TransactionChannel;
import com.finshield.backend.transaction.domain.TransactionType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionIngestRequest(
        @Pattern(regexp = "[A-Za-z0-9._:-]{6,64}") String transactionReference,
        UUID sourceAccountId,
        @Pattern(regexp = "[A-Za-z0-9-]{6,34}") String sourceAccountNumber,
        @Pattern(regexp = "[A-Za-z0-9-]{4,30}") String customerNumber,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{6,34}") String destinationAccountNumber,
        @NotBlank @Size(min = 2, max = 150) String beneficiaryName,
        @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
        @NotNull TransactionType transactionType,
        @NotNull TransactionChannel channel,
        @Size(max = 128) String deviceId,
        @Size(max = 45) @ValidIpAddress String ipAddress,
        @Size(max = 200) String geoLocation,
        @NotNull @PastOrPresent Instant initiatedAt
) {
}
