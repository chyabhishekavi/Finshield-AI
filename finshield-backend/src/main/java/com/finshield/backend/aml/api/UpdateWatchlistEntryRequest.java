package com.finshield.backend.aml.api;

import com.finshield.backend.aml.domain.AmlListType;
import com.finshield.backend.aml.domain.AmlRiskCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateWatchlistEntryRequest(
        @NotBlank @Size(min = 2, max = 200) String name,
        @NotBlank @Size(min = 2, max = 100) String country,
        @NotNull AmlListType listType,
        @NotNull AmlRiskCategory riskCategory,
        @NotNull Boolean active
) {
}
