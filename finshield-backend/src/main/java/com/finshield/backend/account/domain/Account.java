package com.finshield.backend.account.domain;

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
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(
        name = "accounts",
        indexes = {
                @Index(name = "idx_accounts_customer_id", columnList = "customer_id"),
                @Index(name = "idx_accounts_status", columnList = "status"),
                @Index(name = "idx_accounts_type", columnList = "account_type")
        }
)
public class Account extends AuditableEntity {

    @NotBlank
    @Pattern(regexp = "[A-Z0-9-]{6,34}")
    @Column(name = "account_number", nullable = false, unique = true, updatable = false, length = 34)
    private String accountNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @NotNull
    @Digits(integer = 15, fraction = 4)
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @NotBlank
    @Pattern(regexp = "[A-Z]{3}")
    @Column(nullable = false, length = 3)
    private String currency;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @NotNull
    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_accounts_customer")
    )
    private Customer customer;

    protected Account() {
        // Required by JPA.
    }

    public Account(
            String accountNumber,
            AccountType accountType,
            BigDecimal balance,
            String currency,
            AccountStatus status,
            Instant openedAt,
            Customer customer
    ) {
        this.accountNumber = normalize(accountNumber, "accountNumber");
        this.accountType = Objects.requireNonNull(accountType, "accountType must not be null");
        this.balance = Objects.requireNonNull(balance, "balance must not be null");
        this.currency = normalize(currency, "currency");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.openedAt = Objects.requireNonNull(openedAt, "openedAt must not be null");
        this.customer = Objects.requireNonNull(customer, "customer must not be null");
    }

    private static String normalize(String value, String field) {
        return Objects.requireNonNull(value, field + " must not be null").trim().toUpperCase(Locale.ROOT);
    }

    public String getAccountNumber() { return accountNumber; }
    public AccountType getAccountType() { return accountType; }
    public BigDecimal getBalance() { return balance; }
    public String getCurrency() { return currency; }
    public AccountStatus getStatus() { return status; }
    public Instant getOpenedAt() { return openedAt; }
    public Customer getCustomer() { return customer; }
}
