package com.finshield.backend.customer.domain;

import com.finshield.backend.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(
        name = "customers",
        indexes = {
                @Index(name = "idx_customers_full_name", columnList = "full_name"),
                @Index(name = "idx_customers_email", columnList = "email"),
                @Index(name = "idx_customers_kyc_status", columnList = "kyc_status"),
                @Index(name = "idx_customers_risk_level", columnList = "customer_risk_level"),
                @Index(name = "idx_customers_branch_code", columnList = "branch_code")
        }
)
public class Customer extends AuditableEntity {

    @NotBlank
    @Pattern(regexp = "[A-Z0-9-]{4,30}")
    @Column(name = "customer_number", nullable = false, unique = true, updatable = false, length = 30)
    private String customerNumber;

    @NotBlank
    @Size(min = 2, max = 150)
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @NotNull
    @Past
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @NotBlank
    @Email
    @Size(max = 254)
    @Column(nullable = false, length = 254)
    private String email;

    @NotBlank
    @Pattern(regexp = "\\+?[0-9]{7,20}")
    @Column(nullable = false, length = 21)
    private String phone;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 20)
    private KycStatus kycStatus;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_risk_level", nullable = false, length = 20)
    private CustomerRiskLevel customerRiskLevel;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String occupation;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "annual_income_range", nullable = false, length = 30)
    private AnnualIncomeRange annualIncomeRange;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(nullable = false, length = 100)
    private String country;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(nullable = false, length = 100)
    private String city;

    @NotBlank
    @Pattern(regexp = "[A-Z0-9-]{2,20}")
    @Column(name = "branch_code", nullable = false, length = 20)
    private String branchCode;

    protected Customer() {
        // Required by JPA.
    }

    public Customer(
            String customerNumber,
            String fullName,
            LocalDate dateOfBirth,
            String email,
            String phone,
            KycStatus kycStatus,
            CustomerRiskLevel customerRiskLevel,
            String occupation,
            AnnualIncomeRange annualIncomeRange,
            String country,
            String city,
            String branchCode
    ) {
        this.customerNumber = normalizeCode(customerNumber, "customerNumber");
        this.fullName = requireTrimmed(fullName, "fullName");
        this.dateOfBirth = Objects.requireNonNull(dateOfBirth, "dateOfBirth must not be null");
        this.email = normalizeEmail(email);
        this.phone = requireTrimmed(phone, "phone");
        this.kycStatus = Objects.requireNonNull(kycStatus, "kycStatus must not be null");
        this.customerRiskLevel = Objects.requireNonNull(customerRiskLevel, "customerRiskLevel must not be null");
        this.occupation = requireTrimmed(occupation, "occupation");
        this.annualIncomeRange = Objects.requireNonNull(annualIncomeRange, "annualIncomeRange must not be null");
        this.country = requireTrimmed(country, "country");
        this.city = requireTrimmed(city, "city");
        this.branchCode = normalizeCode(branchCode, "branchCode");
    }

    public void changeRiskLevel(CustomerRiskLevel riskLevel) {
        this.customerRiskLevel = Objects.requireNonNull(riskLevel, "riskLevel must not be null");
    }

    @PrePersist
    @PreUpdate
    void normalizeFields() {
        customerNumber = normalizeCode(customerNumber, "customerNumber");
        fullName = requireTrimmed(fullName, "fullName");
        email = normalizeEmail(email);
        phone = requireTrimmed(phone, "phone");
        occupation = requireTrimmed(occupation, "occupation");
        country = requireTrimmed(country, "country");
        city = requireTrimmed(city, "city");
        branchCode = normalizeCode(branchCode, "branchCode");
    }

    private static String normalizeCode(String value, String field) {
        return requireTrimmed(value, field).toUpperCase(Locale.ROOT);
    }

    private static String normalizeEmail(String value) {
        return requireTrimmed(value, "email").toLowerCase(Locale.ROOT);
    }

    private static String requireTrimmed(String value, String field) {
        return Objects.requireNonNull(value, field + " must not be null").trim();
    }

    public String getCustomerNumber() { return customerNumber; }
    public String getFullName() { return fullName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public KycStatus getKycStatus() { return kycStatus; }
    public CustomerRiskLevel getCustomerRiskLevel() { return customerRiskLevel; }
    public String getOccupation() { return occupation; }
    public AnnualIncomeRange getAnnualIncomeRange() { return annualIncomeRange; }
    public String getCountry() { return country; }
    public String getCity() { return city; }
    public String getBranchCode() { return branchCode; }
}
