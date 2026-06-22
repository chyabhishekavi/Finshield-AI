package com.finshield.backend.fraud.alert.event;

import com.finshield.backend.common.entity.BaseEntity;
import com.finshield.backend.fraud.alert.domain.FraudAlert;
import com.finshield.backend.fraud.alert.domain.FraudAlertSeverity;
import com.finshield.backend.fraud.alert.domain.FraudAlertStatus;
import com.finshield.backend.transaction.domain.RiskBand;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "fraud_alert_event_outbox", indexes = @Index(
        name = "idx_fraud_alert_outbox_pending", columnList = "published_at, created_at"))
public class FraudAlertEventOutbox extends BaseEntity {
    @NotNull @Column(name = "event_id", nullable = false, unique = true, updatable = false)
    private UUID eventId;
    @NotNull @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_id", nullable = false, unique = true, updatable = false,
            foreignKey = @ForeignKey(name = "fk_alert_outbox_alert"))
    private FraudAlert alert;
    @NotBlank @Column(name = "alert_number", nullable = false, updatable = false, length = 40)
    private String alertNumber;
    @NotNull @Column(name = "transaction_id", nullable = false, updatable = false) private UUID transactionId;
    @NotBlank @Column(name = "transaction_reference", nullable = false, updatable = false, length = 64)
    private String transactionReference;
    @NotNull @Column(name = "customer_id", nullable = false, updatable = false) private UUID customerId;
    @NotNull @Column(name = "risk_score", nullable = false, updatable = false, precision = 5, scale = 2)
    private BigDecimal riskScore;
    @NotNull @Enumerated(EnumType.STRING) @Column(name = "risk_band", nullable = false, updatable = false, length = 20)
    private RiskBand riskBand;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 20)
    private FraudAlertSeverity severity;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 30)
    private FraudAlertStatus status;
    @NotNull @Column(name = "occurred_at", nullable = false, updatable = false) private Instant occurredAt;
    @Column(name = "published_at") private Instant publishedAt;
    @Column(nullable = false) private int attempts;
    @Size(max = 1000) @Column(name = "last_error", length = 1000) private String lastError;

    protected FraudAlertEventOutbox() {}
    public FraudAlertEventOutbox(UUID eventId, FraudAlert alert) {
        this.eventId = Objects.requireNonNull(eventId);
        this.alert = Objects.requireNonNull(alert);
        this.alertNumber = alert.getAlertNumber();
        this.transactionId = alert.getTransaction().getId();
        this.transactionReference = alert.getTransaction().getTransactionReference();
        this.customerId = alert.getCustomer().getId();
        this.riskScore = alert.getRiskScore();
        this.riskBand = alert.getRiskBand();
        this.severity = alert.getSeverity();
        this.status = alert.getStatus();
        this.occurredAt = alert.getCreatedAt();
    }
    public void published(Instant at) { publishedAt = Objects.requireNonNull(at); attempts++; lastError = null; }
    public void failed(String error) { attempts++; lastError = error == null ? "Unknown error" : error.substring(0, Math.min(1000, error.length())); }
    public UUID getEventId() { return eventId; }
    public FraudAlert getAlert() { return alert; }
    public String getAlertNumber() { return alertNumber; }
    public UUID getTransactionId() { return transactionId; }
    public String getTransactionReference() { return transactionReference; }
    public UUID getCustomerId() { return customerId; }
    public BigDecimal getRiskScore() { return riskScore; }
    public RiskBand getRiskBand() { return riskBand; }
    public FraudAlertSeverity getSeverity() { return severity; }
    public FraudAlertStatus getStatus() { return status; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }
}
