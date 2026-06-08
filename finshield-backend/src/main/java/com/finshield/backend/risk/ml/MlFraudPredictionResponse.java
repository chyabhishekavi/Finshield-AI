package com.finshield.backend.risk.ml;

import com.finshield.backend.transaction.domain.RiskBand;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record MlFraudPredictionResponse(
        BigDecimal fraudProbability,
        RiskBand riskBand,
        List<String> topReasons
) {

    public MlFraudPredictionResponse {
        Objects.requireNonNull(fraudProbability, "fraudProbability must not be null");
        Objects.requireNonNull(riskBand, "riskBand must not be null");
        topReasons = List.copyOf(Objects.requireNonNull(topReasons, "topReasons must not be null"));
        if (fraudProbability.signum() < 0 || fraudProbability.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("fraudProbability must be between 0 and 1");
        }
    }

    public static MlFraudPredictionResponse unavailable() {
        return new MlFraudPredictionResponse(
                BigDecimal.ZERO,
                RiskBand.LOW,
                List.of("ML service unavailable")
        );
    }

    public boolean isUnavailable() {
        return topReasons != null && topReasons.contains("ML service unavailable");
    }
}
