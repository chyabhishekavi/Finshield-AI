package com.finshield.backend.aml.api;

import com.finshield.backend.aml.domain.AmlListType;
import com.finshield.backend.aml.domain.AmlMatchType;
import com.finshield.backend.aml.domain.AmlRiskCategory;
import com.finshield.backend.aml.domain.AmlScreeningResult;
import com.finshield.backend.aml.domain.AmlScreeningStatus;
import com.finshield.backend.aml.domain.AmlSubjectType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AmlScreeningResponse(
        UUID screeningReference,
        AmlSubjectType subjectType,
        UUID subjectId,
        String subjectName,
        boolean matched,
        List<ScreeningMatchResponse> results,
        Instant screenedAt
) {

    public AmlScreeningResponse {
        results = List.copyOf(results);
    }

    public record ScreeningMatchResponse(
            UUID resultId,
            UUID watchlistEntryId,
            String watchlistName,
            String watchlistIdentifier,
            AmlListType listType,
            AmlRiskCategory riskCategory,
            AmlMatchType matchType,
            BigDecimal matchScore,
            AmlScreeningStatus status,
            String reason
    ) {
        public static ScreeningMatchResponse from(AmlScreeningResult result) {
            return new ScreeningMatchResponse(
                    result.getId(),
                    result.getWatchlistEntry() == null ? null : result.getWatchlistEntry().getId(),
                    result.getWatchlistName(),
                    result.getWatchlistIdentifier(),
                    result.getWatchlistType(),
                    result.getWatchlistRiskCategory(),
                    result.getMatchType(),
                    result.getMatchScore(),
                    result.getStatus(),
                    result.getReason()
            );
        }
    }
}
