package com.finshield.backend.aml.api;

import com.finshield.backend.aml.domain.AmlListType;
import com.finshield.backend.aml.domain.AmlRiskCategory;
import com.finshield.backend.aml.domain.AmlWatchlistEntry;

import java.time.Instant;
import java.util.UUID;

public record WatchlistEntryResponse(
        UUID id,
        String name,
        String identifier,
        String country,
        AmlListType listType,
        AmlRiskCategory riskCategory,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public static WatchlistEntryResponse from(AmlWatchlistEntry entry) {
        return new WatchlistEntryResponse(
                entry.getId(), entry.getName(), entry.getIdentifier(), entry.getCountry(),
                entry.getListType(), entry.getRiskCategory(), entry.isActive(),
                entry.getCreatedAt(), entry.getUpdatedAt()
        );
    }
}
