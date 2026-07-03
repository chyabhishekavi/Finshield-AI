package com.finshield.backend.casework.domain;

import com.finshield.backend.common.entity.AuditableEntity;
import com.finshield.backend.customer.domain.Customer;
import com.finshield.backend.fraud.alert.domain.FraudAlert;
import com.finshield.backend.transaction.domain.Transaction;
import com.finshield.backend.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "investigation_cases", indexes = {
        @Index(name = "idx_cases_status_priority", columnList = "status, priority"),
        @Index(name = "idx_cases_customer", columnList = "customer_id"),
        @Index(name = "idx_cases_assigned_to", columnList = "assigned_to_id"),
        @Index(name = "idx_cases_created_at", columnList = "created_at")
})
public class InvestigationCase extends AuditableEntity {

    @NotBlank
    @Size(max = 40)
    @Column(name = "case_number", nullable = false, unique = true, updatable = false, length = 40)
    private String caseNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "case_type", nullable = false, updatable = false, length = 30)
    private InvestigationCaseType caseType;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_case_customer"))
    private Customer customer;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "primary_transaction_id", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_case_primary_transaction"))
    private Transaction primaryTransaction;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_alert_id", unique = true, updatable = false,
            foreignKey = @ForeignKey(name = "fk_case_linked_alert"))
    private FraudAlert linkedAlert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id", foreignKey = @ForeignKey(name = "fk_case_assigned_user"))
    private User assignedTo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CasePriority priority;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private InvestigationCaseStatus status;

    @NotBlank
    @Size(max = 4000)
    @Column(nullable = false, length = 4000)
    private String summary;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CaseDecision decision;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected InvestigationCase() {
    }

    public InvestigationCase(String caseNumber, InvestigationCaseType caseType, Customer customer,
            Transaction primaryTransaction, FraudAlert linkedAlert, CasePriority priority, String summary) {
        this.caseNumber = text(caseNumber, "caseNumber");
        this.caseType = Objects.requireNonNull(caseType);
        this.customer = Objects.requireNonNull(customer);
        this.primaryTransaction = Objects.requireNonNull(primaryTransaction);
        this.linkedAlert = linkedAlert;
        this.priority = Objects.requireNonNull(priority);
        this.status = InvestigationCaseStatus.OPEN;
        this.summary = text(summary, "summary");
        this.decision = CaseDecision.PENDING;
    }

    public void assign(User investigator) {
        requireOpen();
        assignedTo = Objects.requireNonNull(investigator);
        if (status == InvestigationCaseStatus.OPEN) status = InvestigationCaseStatus.UNDER_INVESTIGATION;
    }

    public void changeStatus(InvestigationCaseStatus next) {
        requireOpen();
        Objects.requireNonNull(next, "status must not be null");
        if (next == InvestigationCaseStatus.CLOSED) {
            throw new IllegalArgumentException("Use close operation to close a case");
        }
        if (next == InvestigationCaseStatus.OPEN) {
            throw new IllegalArgumentException("Case cannot transition back to OPEN");
        }
        if (!allowedNextStatuses().contains(next)) {
            throw new IllegalStateException("Invalid case transition from " + status + " to " + next);
        }
        status = next;
    }

    public void recordDecision(CaseDecision decision, String rationale) {
        requireOpen();
        if (decision == CaseDecision.PENDING) throw new IllegalArgumentException("Decision cannot be PENDING");
        this.decision = Objects.requireNonNull(decision);
        append("Decision: " + decision + ". " + text(rationale, "rationale"));
    }

    public void close(CaseDecision decision, String rationale, Instant closedAt) {
        recordDecision(decision, rationale);
        status = InvestigationCaseStatus.CLOSED;
        this.closedAt = Objects.requireNonNull(closedAt);
    }

    private void requireOpen() {
        if (status == InvestigationCaseStatus.CLOSED) throw new IllegalStateException("Closed cases cannot be modified");
    }

    private Set<InvestigationCaseStatus> allowedNextStatuses() {
        return switch (status) {
            case OPEN -> Set.of(InvestigationCaseStatus.UNDER_INVESTIGATION,
                    InvestigationCaseStatus.PENDING_CUSTOMER_VERIFICATION,
                    InvestigationCaseStatus.ESCALATED_TO_COMPLIANCE);
            case UNDER_INVESTIGATION -> Set.of(InvestigationCaseStatus.PENDING_CUSTOMER_VERIFICATION,
                    InvestigationCaseStatus.ESCALATED_TO_COMPLIANCE, InvestigationCaseStatus.REPORTED);
            case PENDING_CUSTOMER_VERIFICATION -> Set.of(InvestigationCaseStatus.UNDER_INVESTIGATION,
                    InvestigationCaseStatus.ESCALATED_TO_COMPLIANCE);
            case ESCALATED_TO_COMPLIANCE -> Set.of(InvestigationCaseStatus.UNDER_INVESTIGATION,
                    InvestigationCaseStatus.REPORTED);
            case REPORTED -> Set.of(InvestigationCaseStatus.UNDER_INVESTIGATION);
            case CLOSED -> Set.of();
        };
    }

    private void append(String value) {
        String combined = summary + "\n" + value;
        summary = combined.length() <= 4000 ? combined : combined.substring(0, 4000);
    }

    private static String text(String value, String field) {
        String result = Objects.requireNonNull(value, field + " must not be null").trim();
        if (result.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return result;
    }

    public String getCaseNumber() { return caseNumber; }
    public InvestigationCaseType getCaseType() { return caseType; }
    public Customer getCustomer() { return customer; }
    public Transaction getPrimaryTransaction() { return primaryTransaction; }
    public FraudAlert getLinkedAlert() { return linkedAlert; }
    public User getAssignedTo() { return assignedTo; }
    public CasePriority getPriority() { return priority; }
    public InvestigationCaseStatus getStatus() { return status; }
    public String getSummary() { return summary; }
    public CaseDecision getDecision() { return decision; }
    public Instant getClosedAt() { return closedAt; }
    public long getVersion() { return version; }
}
