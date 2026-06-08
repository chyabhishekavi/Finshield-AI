package com.finshield.backend.risk.ml;

import java.math.BigDecimal;
import java.util.List;

public class NeutralFallbackMlFraudScoreProvider implements MlFraudScoreProvider {

    private static final BigDecimal FALLBACK_SCORE = BigDecimal.ZERO;

    @Override
    public MlFraudScore score(MlFraudScoringContext context) {
        return new MlFraudScore(
                FALLBACK_SCORE,
                "ml-service-unavailable",
                true,
                List.of("ML service unavailable")
        );
    }
}
