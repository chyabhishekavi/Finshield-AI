package com.finshield.backend.audit.domain;

public enum AuditAction {
    LOGIN,
    FRAUD_RULE_CREATED,
    FRAUD_RULE_UPDATED,
    ALERT_ASSIGNED,
    ALERT_CLOSED,
    CASE_STATUS_CHANGED,
    CASE_ESCALATED,
    TRANSACTION_DECISION_CHANGED
}
