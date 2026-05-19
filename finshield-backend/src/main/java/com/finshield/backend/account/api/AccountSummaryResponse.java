package com.finshield.backend.account.api;

import com.finshield.backend.account.domain.Account;
import com.finshield.backend.account.domain.AccountStatus;
import com.finshield.backend.account.domain.AccountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountSummaryResponse(
        UUID id,
        String accountNumber,
        AccountType accountType,
        BigDecimal balance,
        String currency,
        AccountStatus status,
        Instant openedAt
) {

    public static AccountSummaryResponse from(Account account) {
        return new AccountSummaryResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getCurrency(),
                account.getStatus(),
                account.getOpenedAt()
        );
    }
}
