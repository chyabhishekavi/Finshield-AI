package com.finshield.backend.risk.domain;

import com.finshield.backend.common.entity.AuditableEntity;
import com.finshield.backend.risk.validation.RiskScoreValue;
import com.finshield.backend.transaction.domain.RiskBand;
import com.finshield.backend.transaction.domain.Transaction;
import com.finshield.backend.transaction.domain.TransactionDecision;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "transaction_risk_scores",
        indexes = {
                @Index(name = "idx_transaction_risk_scores_transaction", columnList = "transaction_id"),
                @Index(name = "idx_transaction_risk_scores_band", columnList = "risk_band"),
                @Index(name = "idx_transaction_risk_scores_scored_at", columnList = "scored_at")
        }
)
public class TransactionRiskScore extends AuditableEntity {

    @NotNull
    @Column(name = "source_event_id", nullable = false, unique = true, updatable = false)
    private UUID sourceEventId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "transaction_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_transaction_risk_scores_transaction")
    )
    private Transaction transaction;

    @NotNull
    @RiskScoreValue
    @Column(name = "rule_score", nullable = false, updatable = false, precision = 5, scale = 2)
    private BigDecimal ruleScore;

    @NotNull
    @RiskScoreValue
    @Column(name = "ml_score", nullable = false, updatable = false, precision = 5, scale = 2)
    private BigDecimal mlScore;

    @NotNull
    @RiskScoreValue
    @Column(name = "customer_risk_score", nullable = false, updatable = false, precision = 5, scale = 2)
    private BigDecimal customerRiskScore;

    @NotNull
    @RiskScoreValue
    @Column(name = "device_risk_score", nullable = false, updatable = false, precision = 5, scale = 2)
    private BigDecimal deviceRiskScore;

    @NotNull
    @RiskScoreValue
    @Column(name = "aml_score", nullable = false, updatable = false, precision = 5, scale = 2)
    private BigDecimal amlScore;

    @NotNull
    @RiskScoreValue
    @Column(name = "final_score", nullable = false, updatable = false, precision = 5, scale = 2)
    private BigDecimal finalScore;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_band", nullable = false, updatable = false, length = 20)
    private RiskBand riskBand;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private TransactionDecision decision;

    @NotBlank
    @Column(name = "scoring_version", nullable = false, updatable = false, length = 50)
    private String scoringVersion;

    @NotBlank
    @Column(name = "ml_model_version", nullable = false, updatable = false, length = 100)
    private String mlModelVersion;

    @Column(name = "ml_fallback_used", nullable = false, updatable = false)
    private boolean mlFallbackUsed;

    @NotBlank
    @Size(max = 2000)
    @Column(name = "explanation_summary", nullable = false, updatable = false, length = 2000)
    private String explanationSummary;

    @NotNull
    @Column(name = "scored_at", nullable = false, updatable = false)
    private Instant scoredAt;

    protected TransactionRiskScore() {
        // Required by JPA.
    }

    public TransactionRiskScore(
            UUID sourceEventId,
            Transaction transaction,
            BigDecimal ruleScore,
            BigDecimal mlScore,
            BigDecimal customerRiskScore,
            BigDecimal deviceRiskScore,
            BigDecimal amlScore,
            BigDecimal finalScore,
            RiskBand riskBand,
            TransactionDecision decision,
            String scoringVersion,
            String mlModelVersion,
            boolean mlFallbackUsed,
            String explanationSummary,
            Instant scoredAt
    ) {
        this.sourceEventId = Objects.requireNonNull(sourceEventId, "sourceEventId must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.ruleScore = Objects.requireNonNull(ruleScore, "ruleScore must not be null");
        this.mlScore = Objects.requireNonNull(mlScore, "mlScore must not be null");
        this.customerRiskScore = Objects.requireNonNull(customerRiskScore, "customerRiskScore must not be null");
        this.deviceRiskScore = Objects.requireNonNull(deviceRiskScore, "deviceRiskScore must not be null");
        this.amlScore = Objects.requireNonNull(amlScore, "amlScore must not be null");
        this.finalScore = Objects.requireNonNull(finalScore, "finalScore must not be null");
        this.riskBand = Objects.requireNonNull(riskBand, "riskBand must not be null");
        this.decision = Objects.requireNonNull(decision, "decision must not be null");
        this.scoringVersion = Objects.requireNonNull(scoringVersion, "scoringVersion must not be null");
        this.mlModelVersion = Objects.requireNonNull(mlModelVersion, "mlModelVersion must not be null");
        this.mlFallbackUsed = mlFallbackUsed;
        this.explanationSummary = Objects.requireNonNull(
                explanationSummary, "explanationSummary must not be null");
        this.scoredAt = Objects.requireNonNull(scoredAt, "scoredAt must not be null");
    }

    public UUID getSourceEventId() { return sourceEventId; }
    public Transaction getTransaction() { return transaction; }
    public BigDecimal getRuleScore() { return ruleScore; }
    public BigDecimal getMlScore() { return mlScore; }
    public BigDecimal getCustomerRiskScore() { return customerRiskScore; }
    public BigDecimal getDeviceRiskScore() { return deviceRiskScore; }
    public BigDecimal getAmlScore() { return amlScore; }
    public BigDecimal getFinalScore() { return finalScore; }
    public RiskBand getRiskBand() { return riskBand; }
    public TransactionDecision getDecision() { return decision; }
    public String getScoringVersion() { return scoringVersion; }
    public String getMlModelVersion() { return mlModelVersion; }
    public boolean isMlFallbackUsed() { return mlFallbackUsed; }
    public String getExplanationSummary() { return explanationSummary; }
    public Instant getScoredAt() { return scoredAt; }

}
