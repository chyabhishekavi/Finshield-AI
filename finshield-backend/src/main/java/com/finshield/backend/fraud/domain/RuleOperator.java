package com.finshield.backend.fraud.domain;

public enum RuleOperator {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    IN,
    NOT_IN,
    CONTAINS,
    ALL_MATCH,
    ANY_MATCH
}
