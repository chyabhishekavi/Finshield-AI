package com.finshield.backend.dashboard.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RiskTrendResponse(Instant from, Instant to, String granularity, List<RiskTrendPoint> series) {
    public RiskTrendResponse { series = List.copyOf(series); }
    public record RiskTrendPoint(LocalDate label, long total, long low, long medium,
            long high, long critical, BigDecimal averageRiskScore) {}
}
