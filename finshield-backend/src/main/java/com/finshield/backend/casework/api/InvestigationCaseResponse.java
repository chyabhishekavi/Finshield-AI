package com.finshield.backend.casework.api;

import com.finshield.backend.casework.domain.*;
import java.time.Instant;
import java.util.UUID;

public record InvestigationCaseResponse(
        UUID id, String caseNumber, InvestigationCaseType caseType,
        UUID customerId, String customerNumber, String customerName,
        UUID primaryTransactionId, String transactionReference,
        UUID linkedAlertId, String linkedAlertNumber,
        UUID assignedToId, String assignedToName,
        CasePriority priority, InvestigationCaseStatus status,
        String summary, CaseDecision decision,
        Instant createdAt, Instant updatedAt, Instant closedAt, long version
) {
    public static InvestigationCaseResponse from(InvestigationCase value) {
        return new InvestigationCaseResponse(value.getId(), value.getCaseNumber(), value.getCaseType(),
                value.getCustomer().getId(), value.getCustomer().getCustomerNumber(), value.getCustomer().getFullName(),
                value.getPrimaryTransaction().getId(), value.getPrimaryTransaction().getTransactionReference(),
                value.getLinkedAlert() == null ? null : value.getLinkedAlert().getId(),
                value.getLinkedAlert() == null ? null : value.getLinkedAlert().getAlertNumber(),
                value.getAssignedTo() == null ? null : value.getAssignedTo().getId(),
                value.getAssignedTo() == null ? null : value.getAssignedTo().getFullName(),
                value.getPriority(), value.getStatus(), value.getSummary(), value.getDecision(),
                value.getCreatedAt(), value.getUpdatedAt(), value.getClosedAt(), value.getVersion());
    }
}
