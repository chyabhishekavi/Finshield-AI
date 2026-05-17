package com.finshield.backend.customer.domain;

import com.finshield.backend.common.entity.AuditableEntity;
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

import java.util.Objects;

@Entity
@Table(
        name = "customer_risk_history",
        indexes = @Index(name = "idx_customer_risk_history_customer", columnList = "customer_id, created_at")
)
public class CustomerRiskHistory extends AuditableEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_customer_risk_history_customer")
    )
    private Customer customer;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_risk_level", nullable = false, updatable = false, length = 20)
    private CustomerRiskLevel previousRiskLevel;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "new_risk_level", nullable = false, updatable = false, length = 20)
    private CustomerRiskLevel newRiskLevel;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, updatable = false, length = 500)
    private String reason;

    protected CustomerRiskHistory() {
        // Required by JPA.
    }

    public CustomerRiskHistory(
            Customer customer,
            CustomerRiskLevel previousRiskLevel,
            CustomerRiskLevel newRiskLevel,
            String reason
    ) {
        this.customer = Objects.requireNonNull(customer, "customer must not be null");
        this.previousRiskLevel = Objects.requireNonNull(previousRiskLevel, "previousRiskLevel must not be null");
        this.newRiskLevel = Objects.requireNonNull(newRiskLevel, "newRiskLevel must not be null");
        this.reason = Objects.requireNonNull(reason, "reason must not be null").trim();
    }

    public CustomerRiskLevel getPreviousRiskLevel() { return previousRiskLevel; }
    public CustomerRiskLevel getNewRiskLevel() { return newRiskLevel; }
    public String getReason() { return reason; }
}
