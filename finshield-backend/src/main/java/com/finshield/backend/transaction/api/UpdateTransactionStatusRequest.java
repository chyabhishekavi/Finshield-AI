package com.finshield.backend.transaction.api;

import com.finshield.backend.transaction.domain.TransactionStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTransactionStatusRequest(@NotNull TransactionStatus status) {
}
