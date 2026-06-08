package com.finshield.backend.risk.ml;

import java.math.BigDecimal;
import java.util.List;

public record MlFraudScore(
        BigDecimal score,
        String modelVersion,
        boolean fallbackUsed,
        List<String> explanations
) {
    public MlFraudScore {
        explanations = List.copyOf(explanations);
    }
}
