package com.finshield.backend.dashboard.api;

import com.finshield.backend.transaction.api.TransactionResponse;
import java.util.List;

public record HighRiskTransactionsResponse(long totalHighRiskTransactions,
        List<TransactionResponse> transactions) {
    public HighRiskTransactionsResponse { transactions = List.copyOf(transactions); }
}
