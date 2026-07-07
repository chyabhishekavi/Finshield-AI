package com.finshield.backend.dashboard.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TopRulesResponse(Instant from, List<TopRuleItem> rules) {
    public TopRulesResponse { rules = List.copyOf(rules); }
    public record TopRuleItem(String ruleCode, String ruleName,
            long matchCount, BigDecimal totalScoreImpact) {}
}
