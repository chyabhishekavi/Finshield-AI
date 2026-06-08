package com.finshield.backend.risk.ml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.TimeoutException;

@Component
public class MlFraudClient {

    private static final Logger log = LoggerFactory.getLogger(MlFraudClient.class);

    private final WebClient webClient;
    private final MlClientProperties properties;

    public MlFraudClient(WebClient finshieldMlWebClient, MlClientProperties properties) {
        this.webClient = finshieldMlWebClient;
        this.properties = properties;
    }

    public MlFraudPredictionResponse predict(MlFraudPredictionRequest request) {
        try {
            MlFraudPredictionResponse response = webClient.post()
                    .uri("/predict/fraud")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(MlFraudPredictionResponse.class)
                    .timeout(properties.timeout())
                    .block();
            if (response == null || response.fraudProbability() == null) {
                throw new IllegalStateException("ML service returned an empty response");
            }
            return response;
        } catch (RuntimeException exception) {
            Throwable cause = exception.getCause();
            String failureType = cause instanceof TimeoutException
                    ? "TimeoutException"
                    : exception.getClass().getSimpleName();
            log.warn("ML fraud prediction unavailable; applying zero-score fallback. failureType={}",
                    failureType);
            return MlFraudPredictionResponse.unavailable();
        }
    }
}
