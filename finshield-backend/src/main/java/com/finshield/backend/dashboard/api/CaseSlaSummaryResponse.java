package com.finshield.backend.dashboard.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CaseSlaSummaryResponse(Instant from, Instant to,
        long openCases, long overdueCases, long dueSoonCases,
        long closedWithinSla, long closedAfterSla,
        BigDecimal averageResolutionHours, List<PrioritySlaBucket> byPriority) {
    public CaseSlaSummaryResponse { byPriority = List.copyOf(byPriority); }
    public record PrioritySlaBucket(String label, long open, long overdue,
            long dueSoon, long closedWithinSla, long closedAfterSla) {}
}
