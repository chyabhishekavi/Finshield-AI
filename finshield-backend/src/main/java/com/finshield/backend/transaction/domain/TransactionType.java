package com.finshield.backend.transaction.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TransactionType {
    ACCOUNT_TRANSFER,
    CARD_PURCHASE,
    CASH_DEPOSIT,
    CASH_WITHDRAWAL,
    BILL_PAYMENT,
    INTERNATIONAL_TRANSFER,
    REFUND;

    @JsonCreator
    public static TransactionType fromJson(String value) {
        if ("TRANSFER".equalsIgnoreCase(value)) {
            return ACCOUNT_TRANSFER;
        }
        return valueOf(value.trim().toUpperCase());
    }
}
