package com.finshield.backend.casework.domain;

public enum CaseDecision {
    PENDING,
    FRAUD_CONFIRMED,
    FALSE_POSITIVE,
    SAR_RECOMMENDED,
    ACCOUNT_RESTRICTED,
    NO_ACTION
}
