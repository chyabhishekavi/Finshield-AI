package com.finshield.backend.transaction.domain;

import com.finshield.backend.account.domain.Account;
import com.finshield.backend.common.entity.AuditableEntity;
import com.finshield.backend.device.validation.ValidIpAddress;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(
        name = "financial_transactions",
        indexes = {
                @Index(name = "idx_transactions_source_account", columnList = "source_account_id"),
                @Index(name = "idx_transactions_initiated_at", columnList = "initiated_at"),
                @Index(name = "idx_transactions_status", columnList = "status"),
                @Index(name = "idx_transactions_risk_band", columnList = "risk_band"),
                @Index(name = "idx_transactions_decision", columnList = "decision")
        }
)
public class Transaction extends AuditableEntity {

    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "[A-Z0-9._:-]{6,64}")
    @Column(name = "transaction_reference", nullable = false, unique = true, updatable = false, length = 64)
    private String transactionReference;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "source_account_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_transactions_source_account")
    )
    private Account sourceAccount;

    @NotBlank
    @Pattern(regexp = "[A-Z0-9-]{6,34}")
    @Column(name = "destination_account_number", nullable = false, updatable = false, length = 34)
    private String destinationAccountNumber;

    @NotBlank
    @Size(min = 2, max = 150)
    @Column(name = "beneficiary_name", nullable = false, updatable = false, length = 150)
    private String beneficiaryName;

    @NotNull
    @Positive
    @Digits(integer = 15, fraction = 4)
    @Column(nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @NotBlank
    @Pattern(regexp = "[A-Z]{3}")
    @Column(nullable = false, updatable = false, length = 3)
    private String currency;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, updatable = false, length = 30)
    private TransactionType transactionType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private TransactionChannel channel;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionStatus status;

    @Size(max = 128)
    @Column(name = "device_id", updatable = false, length = 128)
    private String deviceId;

    @ValidIpAddress
    @Size(max = 45)
    @Column(name = "ip_address", updatable = false, length = 45)
    private String ipAddress;

    @Size(max = 200)
    @Column(name = "geo_location", updatable = false, length = 200)
    private String geoLocation;

    @NotNull
    @PastOrPresent
    @Column(name = "initiated_at", nullable = false, updatable = false)
    private Instant initiatedAt;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2)
    @Column(name = "risk_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal riskScore;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_band", nullable = false, length = 20)
    private RiskBand riskBand;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionDecision decision;

    protected Transaction() {
        // Required by JPA.
    }

    public Transaction(
            String transactionReference,
            Account sourceAccount,
            String destinationAccountNumber,
            String beneficiaryName,
            BigDecimal amount,
            String currency,
            TransactionType transactionType,
            TransactionChannel channel,
            TransactionStatus status,
            String deviceId,
            String ipAddress,
            String geoLocation,
            Instant initiatedAt
    ) {
        this.transactionReference = normalize(transactionReference, "transactionReference");
        this.sourceAccount = Objects.requireNonNull(sourceAccount, "sourceAccount must not be null");
        this.destinationAccountNumber = normalize(destinationAccountNumber, "destinationAccountNumber");
        this.beneficiaryName = requireTrimmed(beneficiaryName, "beneficiaryName");
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        this.currency = normalize(currency, "currency");
        this.transactionType = Objects.requireNonNull(transactionType, "transactionType must not be null");
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.deviceId = trimToNull(deviceId);
        this.ipAddress = trimToNull(ipAddress);
        this.geoLocation = trimToNull(geoLocation);
        this.initiatedAt = Objects.requireNonNull(initiatedAt, "initiatedAt must not be null");
        this.riskScore = BigDecimal.ZERO.setScale(2);
        this.riskBand = RiskBand.LOW;
        this.decision = TransactionDecision.PENDING;
    }

    public void applyRiskDecision(
            BigDecimal riskScore,
            RiskBand riskBand,
            TransactionDecision decision
    ) {
        this.riskScore = Objects.requireNonNull(riskScore, "riskScore must not be null");
        this.riskBand = Objects.requireNonNull(riskBand, "riskBand must not be null");
        this.decision = Objects.requireNonNull(decision, "decision must not be null");
    }

    public void transitionTo(TransactionStatus nextStatus) {
        if (!status.canTransitionTo(nextStatus)) {
            throw new IllegalStateException("Invalid transaction status transition from " + status + " to " + nextStatus);
        }
        this.status = nextStatus;
    }

    private static String normalize(String value, String field) {
        return requireTrimmed(value, field).toUpperCase(Locale.ROOT);
    }

    private static String requireTrimmed(String value, String field) {
        return Objects.requireNonNull(value, field + " must not be null").trim();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public String getTransactionReference() { return transactionReference; }
    public Account getSourceAccount() { return sourceAccount; }
    public String getDestinationAccountNumber() { return destinationAccountNumber; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public TransactionType getTransactionType() { return transactionType; }
    public TransactionChannel getChannel() { return channel; }
    public TransactionStatus getStatus() { return status; }
    public String getDeviceId() { return deviceId; }
    public String getIpAddress() { return ipAddress; }
    public String getGeoLocation() { return geoLocation; }
    public Instant getInitiatedAt() { return initiatedAt; }
    public BigDecimal getRiskScore() { return riskScore; }
    public RiskBand getRiskBand() { return riskBand; }
    public TransactionDecision getDecision() { return decision; }
}
