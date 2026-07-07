package com.finshield.backend.dashboard.api;

import java.time.Instant;

public record DashboardSummaryResponse(
        long totalTransactions,
        long highRiskTransactions,
        long totalAlerts,
        long openAlerts,
        long criticalAlerts,
        long totalCases,
        long openCases,
        Instant generatedAt
) {}
