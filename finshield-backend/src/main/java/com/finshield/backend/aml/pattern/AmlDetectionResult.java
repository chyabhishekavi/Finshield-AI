package com.finshield.backend.aml.pattern;

import java.math.BigDecimal;
import java.util.List;

public record AmlDetectionResult(
        BigDecimal amlScore,
        List<AmlPatternMatch> matchedPatterns,
        String explanation
) {
    public AmlDetectionResult {
        matchedPatterns = List.copyOf(matchedPatterns);
    }
}
