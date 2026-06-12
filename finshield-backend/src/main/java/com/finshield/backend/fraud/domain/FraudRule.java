package com.finshield.backend.fraud.domain;

import com.finshield.backend.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(
        name = "fraud_rules",
        indexes = {
                @Index(name = "idx_fraud_rules_active", columnList = "active"),
                @Index(name = "idx_fraud_rules_type", columnList = "rule_type"),
                @Index(name = "idx_fraud_rules_severity", columnList = "severity")
        }
)
public class FraudRule extends AuditableEntity {

    @NotBlank
    @Pattern(regexp = "[A-Z][A-Z0-9_]{2,49}")
    @Column(name = "rule_code", nullable = false, unique = true, updatable = false, length = 50)
    private String ruleCode;

    @NotBlank
    @Size(min = 3, max = 150)
    @Column(name = "rule_name", nullable = false, length = 150)
    private String ruleName;

    @NotBlank
    @Size(max = 1000)
    @Column(nullable = false, length = 1000)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 30)
    private FraudRuleType ruleType;

    @NotBlank
    @Size(max = 150)
    @Column(name = "condition_field", nullable = false, length = 150)
    private String conditionField;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_operator", nullable = false, length = 30)
    private RuleOperator operator;

    @NotBlank
    @Size(max = 500)
    @Column(name = "threshold_value", nullable = false, length = 500)
    private String thresholdValue;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2)
    @Column(name = "score_impact", nullable = false, precision = 5, scale = 2)
    private BigDecimal scoreImpact;

    @Column(nullable = false)
    private boolean active;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RuleSeverity severity;

    protected FraudRule() {
        // Required by JPA.
    }

    public FraudRule(
            String ruleCode,
            String ruleName,
            String description,
            FraudRuleType ruleType,
            String conditionField,
            RuleOperator operator,
            String thresholdValue,
            BigDecimal scoreImpact,
            boolean active,
            RuleSeverity severity
    ) {
        this.ruleCode = normalizeCode(ruleCode);
        update(
                ruleName,
                description,
                ruleType,
                conditionField,
                operator,
                thresholdValue,
                scoreImpact,
                active,
                severity
        );
    }

    public void update(
            String ruleName,
            String description,
            FraudRuleType ruleType,
            String conditionField,
            RuleOperator operator,
            String thresholdValue,
            BigDecimal scoreImpact,
            boolean active,
            RuleSeverity severity
    ) {
        this.ruleName = requireTrimmed(ruleName, "ruleName");
        this.description = requireTrimmed(description, "description");
        this.ruleType = Objects.requireNonNull(ruleType, "ruleType must not be null");
        this.conditionField = requireTrimmed(conditionField, "conditionField");
        this.operator = Objects.requireNonNull(operator, "operator must not be null");
        this.thresholdValue = requireTrimmed(thresholdValue, "thresholdValue");
        this.scoreImpact = Objects.requireNonNull(scoreImpact, "scoreImpact must not be null");
        this.active = active;
        this.severity = Objects.requireNonNull(severity, "severity must not be null");
    }

    public void deactivate() {
        this.active = false;
    }

    private static String normalizeCode(String value) {
        return requireTrimmed(value, "ruleCode").toUpperCase(Locale.ROOT);
    }

    private static String requireTrimmed(String value, String field) {
        return Objects.requireNonNull(value, field + " must not be null").trim();
    }

    public String getRuleCode() { return ruleCode; }
    public String getRuleName() { return ruleName; }
    public String getDescription() { return description; }
    public FraudRuleType getRuleType() { return ruleType; }
    public String getConditionField() { return conditionField; }
    public RuleOperator getOperator() { return operator; }
    public String getThresholdValue() { return thresholdValue; }
    public BigDecimal getScoreImpact() { return scoreImpact; }
    public boolean isActive() { return active; }
    public RuleSeverity getSeverity() { return severity; }
}
