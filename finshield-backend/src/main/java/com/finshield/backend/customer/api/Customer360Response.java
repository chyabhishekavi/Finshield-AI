package com.finshield.backend.customer.api;

import com.finshield.backend.account.api.AccountSummaryResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record Customer360Response(
        CustomerResponse customer,
        List<AccountSummaryResponse> accounts,
        int totalAccounts,
        Map<String, BigDecimal> balancesByCurrency,
        List<CustomerRiskHistoryResponse> recentRiskHistory
) {

    public Customer360Response {
        accounts = List.copyOf(accounts);
        balancesByCurrency = Map.copyOf(balancesByCurrency);
        recentRiskHistory = List.copyOf(recentRiskHistory);
    }
}
