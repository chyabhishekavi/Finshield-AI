package com.finshield.backend.risk.domain;

import com.finshield.backend.common.entity.AuditableEntity;
import com.finshield.backend.fraud.domain.RuleSeverity;
import com.finshield.backend.risk.validation.RiskScoreValue;
import com.finshield.backend.transaction.domain.Transaction;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(
        name = "transaction_rule_matches",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_transaction_rule_matches_score_rule",
                columnNames = {"transaction_risk_score_id", "rule_code"}
        ),
        indexes = {
                @Index(name = "idx_transaction_rule_matches_transaction", columnList = "transaction_id"),
                @Index(name = "idx_transaction_rule_matches_rule_code", columnList = "rule_code"),
                @Index(name = "idx_transaction_rule_matches_severity", columnList = "severity")
        }
)
public class TransactionRuleMatch extends AuditableEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "transaction_risk_score_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_transaction_rule_matches_risk_score")
    )
    private TransactionRiskScore transactionRiskScore;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "transaction_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_transaction_rule_matches_transaction")
    )
    private Transaction transaction;

    @NotBlank
    @Pattern(regexp = "[A-Z][A-Z0-9_]{2,49}")
    @Column(name = "rule_code", nullable = false, updatable = false, length = 50)
    private String ruleCode;

    @NotBlank
    @Size(max = 150)
    @Column(name = "rule_name", nullable = false, updatable = false, length = 150)
    private String ruleName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private RuleSeverity severity;

    @NotNull
    @RiskScoreValue
    @Column(name = "score_impact", nullable = false, updatable = false, precision = 5, scale = 2)
    private BigDecimal scoreImpact;

    @NotBlank
    @Size(max = 1200)
    @Column(nullable = false, updatable = false, length = 1200)
    private String reason;

    protected TransactionRuleMatch() {
        // Required by JPA.
    }

    public TransactionRuleMatch(
            TransactionRiskScore transactionRiskScore,
            Transaction transaction,
            String ruleCode,
            String ruleName,
            RuleSeverity severity,
            BigDecimal scoreImpact,
            String reason
    ) {
        this.transactionRiskScore = Objects.requireNonNull(
                transactionRiskScore, "transactionRiskScore must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.ruleCode = Objects.requireNonNull(ruleCode, "ruleCode must not be null");
        this.ruleName = Objects.requireNonNull(ruleName, "ruleName must not be null");
        this.severity = Objects.requireNonNull(severity, "severity must not be null");
        this.scoreImpact = Objects.requireNonNull(scoreImpact, "scoreImpact must not be null");
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public TransactionRiskScore getTransactionRiskScore() { return transactionRiskScore; }
    public Transaction getTransaction() { return transaction; }
    public String getRuleCode() { return ruleCode; }
    public String getRuleName() { return ruleName; }
    public RuleSeverity getSeverity() { return severity; }
    public BigDecimal getScoreImpact() { return scoreImpact; }
    public String getReason() { return reason; }
}
