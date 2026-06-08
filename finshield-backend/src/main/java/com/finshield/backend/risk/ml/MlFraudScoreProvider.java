package com.finshield.backend.risk.ml;

public interface MlFraudScoreProvider {

    MlFraudScore score(MlFraudScoringContext context);
}
