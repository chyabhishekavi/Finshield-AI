package com.finshield.backend.transaction.domain;

import java.util.EnumSet;
import java.util.Set;

public enum TransactionStatus {
    RECEIVED,
    PROCESSING,
    PENDING_REVIEW,
    AUTHORIZED,
    DECLINED,
    COMPLETED,
    FAILED,
    REVERSED,
    CANCELLED;

    public boolean canTransitionTo(TransactionStatus next) {
        return allowedTransitions().contains(next);
    }

    private Set<TransactionStatus> allowedTransitions() {
        return switch (this) {
            case RECEIVED -> EnumSet.of(PROCESSING, PENDING_REVIEW, AUTHORIZED, DECLINED, FAILED, CANCELLED);
            case PROCESSING -> EnumSet.of(PENDING_REVIEW, AUTHORIZED, DECLINED, FAILED, CANCELLED);
            case PENDING_REVIEW -> EnumSet.of(AUTHORIZED, DECLINED, FAILED, CANCELLED);
            case AUTHORIZED -> EnumSet.of(COMPLETED, REVERSED, FAILED);
            case COMPLETED -> EnumSet.of(REVERSED);
            case DECLINED, FAILED, REVERSED, CANCELLED -> EnumSet.noneOf(TransactionStatus.class);
        };
    }
}
