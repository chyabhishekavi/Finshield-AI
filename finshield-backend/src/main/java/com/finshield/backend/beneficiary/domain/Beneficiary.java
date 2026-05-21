package com.finshield.backend.beneficiary.domain;

import com.finshield.backend.common.entity.AuditableEntity;
import com.finshield.backend.customer.domain.Customer;
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
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(
        name = "beneficiaries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_beneficiaries_customer_account",
                columnNames = {"customer_id", "beneficiary_account_number"}
        ),
        indexes = {
                @Index(name = "idx_beneficiaries_customer", columnList = "customer_id"),
                @Index(name = "idx_beneficiaries_status", columnList = "status"),
                @Index(name = "idx_beneficiaries_risk_score", columnList = "risk_score")
        }
)
public class Beneficiary extends AuditableEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_beneficiaries_customer")
    )
    private Customer customer;

    @NotBlank
    @Size(min = 2, max = 150)
    @Column(name = "beneficiary_name", nullable = false, length = 150)
    private String beneficiaryName;

    @NotBlank
    @Pattern(regexp = "[A-Z0-9-]{6,34}")
    @Column(name = "beneficiary_account_number", nullable = false, updatable = false, length = 34)
    private String beneficiaryAccountNumber;

    @NotBlank
    @Size(min = 2, max = 150)
    @Column(name = "bank_name", nullable = false, length = 150)
    private String bankName;

    @NotBlank
    @Pattern(regexp = "[A-Z]{4}0[A-Z0-9]{6}")
    @Column(name = "ifsc_code", nullable = false, length = 11)
    private String ifscCode;

    @NotNull
    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2)
    @Column(name = "risk_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal riskScore;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BeneficiaryStatus status;

    protected Beneficiary() {
        // Required by JPA.
    }

    public Beneficiary(
            Customer customer,
            String beneficiaryName,
            String beneficiaryAccountNumber,
            String bankName,
            String ifscCode,
            Instant addedAt,
            BigDecimal riskScore,
            BeneficiaryStatus status
    ) {
        this.customer = Objects.requireNonNull(customer, "customer must not be null");
        this.beneficiaryName = requireTrimmed(beneficiaryName, "beneficiaryName");
        this.beneficiaryAccountNumber = normalize(beneficiaryAccountNumber, "beneficiaryAccountNumber");
        this.bankName = requireTrimmed(bankName, "bankName");
        this.ifscCode = normalize(ifscCode, "ifscCode");
        this.addedAt = Objects.requireNonNull(addedAt, "addedAt must not be null");
        this.riskScore = Objects.requireNonNull(riskScore, "riskScore must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public void updateProfile(String beneficiaryName, String bankName, String ifscCode) {
        this.beneficiaryName = requireTrimmed(beneficiaryName, "beneficiaryName");
        this.bankName = requireTrimmed(bankName, "bankName");
        this.ifscCode = normalize(ifscCode, "ifscCode");
    }

    public void updateRisk(BigDecimal riskScore, BeneficiaryStatus status) {
        this.riskScore = Objects.requireNonNull(riskScore, "riskScore must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    private static String normalize(String value, String field) {
        return requireTrimmed(value, field).toUpperCase(Locale.ROOT);
    }

    private static String requireTrimmed(String value, String field) {
        return Objects.requireNonNull(value, field + " must not be null").trim();
    }

    public Customer getCustomer() { return customer; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public String getBeneficiaryAccountNumber() { return beneficiaryAccountNumber; }
    public String getBankName() { return bankName; }
    public String getIfscCode() { return ifscCode; }
    public Instant getAddedAt() { return addedAt; }
    public BigDecimal getRiskScore() { return riskScore; }
    public BeneficiaryStatus getStatus() { return status; }
}
