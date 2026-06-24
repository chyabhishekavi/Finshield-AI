package com.finshield.backend.aml.domain;

import com.finshield.backend.beneficiary.domain.Beneficiary;
import com.finshield.backend.common.entity.AuditableEntity;
import com.finshield.backend.customer.domain.Customer;
import com.finshield.backend.risk.validation.RiskScoreValue;
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
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "aml_screening_results",
        indexes = {
                @Index(name = "idx_aml_screening_reference", columnList = "screening_reference"),
                @Index(name = "idx_aml_screening_customer", columnList = "customer_id"),
                @Index(name = "idx_aml_screening_beneficiary", columnList = "beneficiary_id"),
                @Index(name = "idx_aml_screening_status", columnList = "status")
        }
)
public class AmlScreeningResult extends AuditableEntity {

    @NotNull
    @Column(name = "screening_reference", nullable = false, updatable = false)
    private UUID screeningReference;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, updatable = false, length = 20)
    private AmlSubjectType subjectType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", updatable = false,
            foreignKey = @ForeignKey(name = "fk_aml_screening_customer"))
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id", updatable = false,
            foreignKey = @ForeignKey(name = "fk_aml_screening_beneficiary"))
    private Beneficiary beneficiary;

    @NotBlank
    @Size(max = 200)
    @Column(name = "subject_name", nullable = false, updatable = false, length = 200)
    private String subjectName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "watchlist_entry_id", updatable = false,
            foreignKey = @ForeignKey(name = "fk_aml_screening_watchlist_entry"))
    private AmlWatchlistEntry watchlistEntry;

    @Column(name = "watchlist_name", updatable = false, length = 200)
    private String watchlistName;

    @Column(name = "watchlist_identifier", updatable = false, length = 100)
    private String watchlistIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "watchlist_type", updatable = false, length = 30)
    private AmlListType watchlistType;

    @Enumerated(EnumType.STRING)
    @Column(name = "watchlist_risk_category", updatable = false, length = 20)
    private AmlRiskCategory watchlistRiskCategory;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, updatable = false, length = 20)
    private AmlMatchType matchType;

    @NotNull
    @RiskScoreValue
    @Column(name = "match_score", nullable = false, updatable = false, precision = 5, scale = 2)
    private BigDecimal matchScore;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AmlScreeningStatus status;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, updatable = false, length = 500)
    private String reason;

    @NotNull
    @Column(name = "screened_at", nullable = false, updatable = false)
    private Instant screenedAt;

    protected AmlScreeningResult() {
        // Required by JPA.
    }

    public AmlScreeningResult(
            UUID screeningReference,
            AmlSubjectType subjectType,
            Customer customer,
            Beneficiary beneficiary,
            String subjectName,
            AmlWatchlistEntry watchlistEntry,
            AmlMatchType matchType,
            BigDecimal matchScore,
            AmlScreeningStatus status,
            String reason,
            Instant screenedAt
    ) {
        this.screeningReference = Objects.requireNonNull(screeningReference);
        this.subjectType = Objects.requireNonNull(subjectType);
        this.customer = customer;
        this.beneficiary = beneficiary;
        this.subjectName = Objects.requireNonNull(subjectName);
        this.watchlistEntry = watchlistEntry;
        this.watchlistName = watchlistEntry == null ? null : watchlistEntry.getName();
        this.watchlistIdentifier = watchlistEntry == null ? null : watchlistEntry.getIdentifier();
        this.watchlistType = watchlistEntry == null ? null : watchlistEntry.getListType();
        this.watchlistRiskCategory = watchlistEntry == null ? null : watchlistEntry.getRiskCategory();
        this.matchType = Objects.requireNonNull(matchType);
        this.matchScore = Objects.requireNonNull(matchScore);
        this.status = Objects.requireNonNull(status);
        this.reason = Objects.requireNonNull(reason);
        this.screenedAt = Objects.requireNonNull(screenedAt);
    }

    @AssertTrue(message = "screening subject reference must match subject type")
    boolean isSubjectReferenceValid() {
        return subjectType == AmlSubjectType.CUSTOMER
                ? customer != null && beneficiary == null
                : beneficiary != null && customer == null;
    }

    public UUID getScreeningReference() { return screeningReference; }
    public AmlSubjectType getSubjectType() { return subjectType; }
    public Customer getCustomer() { return customer; }
    public Beneficiary getBeneficiary() { return beneficiary; }
    public String getSubjectName() { return subjectName; }
    public AmlWatchlistEntry getWatchlistEntry() { return watchlistEntry; }
    public String getWatchlistName() { return watchlistName; }
    public String getWatchlistIdentifier() { return watchlistIdentifier; }
    public AmlListType getWatchlistType() { return watchlistType; }
    public AmlRiskCategory getWatchlistRiskCategory() { return watchlistRiskCategory; }
    public AmlMatchType getMatchType() { return matchType; }
    public BigDecimal getMatchScore() { return matchScore; }
    public AmlScreeningStatus getStatus() { return status; }
    public String getReason() { return reason; }
    public Instant getScreenedAt() { return screenedAt; }
}
