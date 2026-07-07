package com.finshield.backend.dashboard.api;

import java.util.List;

public record AlertStatusCountResponse(List<StatusCount> series) {
    public AlertStatusCountResponse { series = List.copyOf(series); }
    public record StatusCount(String label, long value) {}
}
