package com.finshield.backend.fraud.alert.domain;

public enum FraudAlertStatus {
    NEW,
    ASSIGNED,
    IN_REVIEW,
    ESCALATED,
    CLOSED_FRAUD,
    CLOSED_FALSE_POSITIVE,
    CONVERTED_TO_CASE;

    public boolean isTerminal() {
        return this == CLOSED_FRAUD
                || this == CLOSED_FALSE_POSITIVE
                || this == CONVERTED_TO_CASE;
    }
}
