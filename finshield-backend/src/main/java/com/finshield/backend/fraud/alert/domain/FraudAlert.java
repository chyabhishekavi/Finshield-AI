package com.finshield.backend.fraud.alert.domain;

import com.finshield.backend.common.entity.AuditableEntity;
import com.finshield.backend.customer.domain.Customer;
import com.finshield.backend.risk.validation.RiskScoreValue;
import com.finshield.backend.transaction.domain.RiskBand;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "fraud_alerts",
        indexes = {
                @Index(name = "idx_fraud_alert_status_severity", columnList = "status, severity"),
                @Index(name = "idx_fraud_alert_assigned_to", columnList = "assigned_to_id"),
                @Index(name = "idx_fraud_alert_customer", columnList = "customer_id"),
                @Index(name = "idx_fraud_alert_due_at", columnList = "due_at")
        }
)
public class FraudAlert extends AuditableEntity {

    @NotBlank
    @Size(max = 40)
    @Column(name = "alert_number", nullable = false, unique = true, updatable = false, length = 40)
    private String alertNumber;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false, unique = true, updatable = false,
            foreignKey = @ForeignKey(name = "fk_fraud_alert_transaction"))
    private Transaction transaction;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_fraud_alert_customer"))
    private Customer customer;

    @NotNull
    @RiskScoreValue
    @Column(name = "risk_score", nullable = false, updatable = false, precision = 5, scale = 2)
    private BigDecimal riskScore;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_band", nullable = false, updatable = false, length = 20)
    private RiskBand riskBand;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FraudAlertSeverity severity;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FraudAlertStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id",
            foreignKey = @ForeignKey(name = "fk_fraud_alert_assigned_user"))
    private User assignedTo;

    @NotNull
    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @NotBlank
    @Size(max = 2000)
    @Column(name = "alert_summary", nullable = false, length = 2000)
    private String alertSummary;

    @Version
    @Column(nullable = false)
    private long version;

    protected FraudAlert() {
        // Required by JPA.
    }

    public FraudAlert(
            String alertNumber,
            Transaction transaction,
            Customer customer,
            BigDecimal riskScore,
            RiskBand riskBand,
            FraudAlertSeverity severity,
            Instant dueAt,
            String alertSummary
    ) {
        this.alertNumber = requireText(alertNumber, "alertNumber");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.customer = Objects.requireNonNull(customer, "customer must not be null");
        this.riskScore = Objects.requireNonNull(riskScore, "riskScore must not be null");
        this.riskBand = Objects.requireNonNull(riskBand, "riskBand must not be null");
        this.severity = Objects.requireNonNull(severity, "severity must not be null");
        this.status = FraudAlertStatus.NEW;
        this.dueAt = Objects.requireNonNull(dueAt, "dueAt must not be null");
        this.alertSummary = requireText(alertSummary, "alertSummary");
    }

    public void assignTo(User analyst, Instant revisedDueAt) {
        requireOpen();
        this.assignedTo = Objects.requireNonNull(analyst, "analyst must not be null");
        if (revisedDueAt != null) {
            this.dueAt = revisedDueAt;
        }
        if (status == FraudAlertStatus.NEW) {
            status = FraudAlertStatus.ASSIGNED;
        }
    }

    public void startReview() {
        requireOpen();
        if (assignedTo == null) {
            throw new IllegalStateException("Alert must be assigned before review can begin");
        }
        if (status != FraudAlertStatus.ASSIGNED && status != FraudAlertStatus.ESCALATED) {
            throw new IllegalStateException("Only assigned or escalated alerts can enter review");
        }
        status = FraudAlertStatus.IN_REVIEW;
    }

    public void escalate(String reason, Instant revisedDueAt) {
        requireOpen();
        if (revisedDueAt != null) {
            dueAt = revisedDueAt;
        }
        severity = FraudAlertSeverity.CRITICAL;
        status = FraudAlertStatus.ESCALATED;
        appendSummary("Escalated: " + requireText(reason, "reason"));
    }

    public void close(FraudAlertStatus closureStatus, String resolution) {
        requireOpen();
        if (closureStatus != FraudAlertStatus.CLOSED_FRAUD
                && closureStatus != FraudAlertStatus.CLOSED_FALSE_POSITIVE) {
            throw new IllegalArgumentException("Invalid fraud alert closure status");
        }
        status = closureStatus;
        appendSummary("Resolution: " + requireText(resolution, "resolution"));
    }

    public void convertToCase(String reason) {
        requireOpen();
        status = FraudAlertStatus.CONVERTED_TO_CASE;
        appendSummary("Converted to case: " + requireText(reason, "reason"));
    }

    private void requireOpen() {
        if (status.isTerminal()) {
            throw new IllegalStateException("Terminal fraud alerts cannot be modified");
        }
    }

    private void appendSummary(String text) {
        String combined = alertSummary + "\n" + text;
        alertSummary = combined.length() <= 2000 ? combined : combined.substring(0, 2000);
    }

    private static String requireText(String value, String field) {
        String text = Objects.requireNonNull(value, field + " must not be null").trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return text;
    }

    public String getAlertNumber() { return alertNumber; }
    public Transaction getTransaction() { return transaction; }
    public Customer getCustomer() { return customer; }
    public BigDecimal getRiskScore() { return riskScore; }
    public RiskBand getRiskBand() { return riskBand; }
    public FraudAlertSeverity getSeverity() { return severity; }
    public FraudAlertStatus getStatus() { return status; }
    public User getAssignedTo() { return assignedTo; }
    public Instant getDueAt() { return dueAt; }
    public String getAlertSummary() { return alertSummary; }
    public long getVersion() { return version; }
}
