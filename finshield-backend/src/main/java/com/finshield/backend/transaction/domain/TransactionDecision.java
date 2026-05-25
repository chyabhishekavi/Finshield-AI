package com.finshield.backend.transaction.domain;

public enum TransactionDecision {
    PENDING,
    APPROVE,
    MONITOR,
    CREATE_ALERT,
    HOLD_AND_ESCALATE
}
