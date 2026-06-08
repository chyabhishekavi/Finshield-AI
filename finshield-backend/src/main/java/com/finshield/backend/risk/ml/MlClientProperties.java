package com.finshield.backend.risk.ml;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "finshield.ml")
public record MlClientProperties(
        @NotBlank String baseUrl,
        @NotNull Duration timeout
) {
    public MlClientProperties {
        if (timeout != null && (timeout.isZero() || timeout.isNegative())) {
            throw new IllegalArgumentException("finshield.ml.timeout must be positive");
        }
    }
}
