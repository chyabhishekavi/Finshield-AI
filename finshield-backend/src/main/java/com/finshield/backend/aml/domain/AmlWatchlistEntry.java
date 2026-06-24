package com.finshield.backend.aml.domain;

import com.finshield.backend.aml.matching.NameNormalizer;
import com.finshield.backend.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Locale;
import java.util.Objects;

@Entity
@Table(
        name = "aml_watchlist_entries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_aml_watchlist_type_identifier",
                columnNames = {"list_type", "identifier"}
        ),
        indexes = {
                @Index(name = "idx_aml_watchlist_normalized_name", columnList = "normalized_name"),
                @Index(name = "idx_aml_watchlist_active_type", columnList = "active, list_type"),
                @Index(name = "idx_aml_watchlist_country", columnList = "country")
        }
)
public class AmlWatchlistEntry extends AuditableEntity {

    @NotBlank
    @Size(min = 2, max = 200)
    @Column(nullable = false, length = 200)
    private String name;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String normalizedName;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String identifier;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(nullable = false, length = 100)
    private String country;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "list_type", nullable = false, length = 30)
    private AmlListType listType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_category", nullable = false, length = 20)
    private AmlRiskCategory riskCategory;

    @Column(nullable = false)
    private boolean active;

    protected AmlWatchlistEntry() {
        // Required by JPA.
    }

    public AmlWatchlistEntry(
            String name,
            String identifier,
            String country,
            AmlListType listType,
            AmlRiskCategory riskCategory,
            boolean active
    ) {
        this.identifier = normalizeIdentifier(identifier);
        update(name, country, listType, riskCategory, active);
    }

    public void update(
            String name,
            String country,
            AmlListType listType,
            AmlRiskCategory riskCategory,
            boolean active
    ) {
        this.name = requireTrimmed(name, "name");
        this.country = requireTrimmed(country, "country");
        this.listType = Objects.requireNonNull(listType, "listType must not be null");
        this.riskCategory = Objects.requireNonNull(riskCategory, "riskCategory must not be null");
        this.active = active;
        this.normalizedName = NameNormalizer.normalize(this.name);
    }

    public void deactivate() {
        this.active = false;
    }

    @PrePersist
    @PreUpdate
    void normalizeFields() {
        name = requireTrimmed(name, "name");
        normalizedName = NameNormalizer.normalize(name);
        identifier = normalizeIdentifier(identifier);
        country = requireTrimmed(country, "country");
    }

    private static String normalizeIdentifier(String value) {
        return requireTrimmed(value, "identifier").toUpperCase(Locale.ROOT);
    }

    private static String requireTrimmed(String value, String field) {
        return Objects.requireNonNull(value, field + " must not be null").trim();
    }

    public String getName() { return name; }
    public String getNormalizedName() { return normalizedName; }
    public String getIdentifier() { return identifier; }
    public String getCountry() { return country; }
    public AmlListType getListType() { return listType; }
    public AmlRiskCategory getRiskCategory() { return riskCategory; }
    public boolean isActive() { return active; }
}
