package com.finshield.backend.customer.api;

import com.finshield.backend.customer.domain.CustomerRiskHistory;
import com.finshield.backend.customer.domain.CustomerRiskLevel;

import java.time.Instant;
import java.util.UUID;

public record CustomerRiskHistoryResponse(
        UUID id,
        CustomerRiskLevel previousRiskLevel,
        CustomerRiskLevel newRiskLevel,
        String reason,
        String changedBy,
        Instant changedAt
) {

    public static CustomerRiskHistoryResponse from(CustomerRiskHistory history) {
        return new CustomerRiskHistoryResponse(
                history.getId(),
                history.getPreviousRiskLevel(),
                history.getNewRiskLevel(),
                history.getReason(),
                history.getCreatedBy(),
                history.getCreatedAt()
        );
    }
}
