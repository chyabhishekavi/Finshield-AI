package com.finshield.backend.risk.ml;

import com.finshield.backend.customer.domain.Customer;
import com.finshield.backend.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Locale;

@Component
public class WebClientMlFraudScoreProvider implements MlFraudScoreProvider {

    private static final String MODEL_VERSION = "finshield-ml-service";
    private static final String FALLBACK_VERSION = "ml-service-unavailable";

    private final MlFraudClient mlFraudClient;
    private final TransactionRepository transactionRepository;

    public WebClientMlFraudScoreProvider(
            MlFraudClient mlFraudClient,
            TransactionRepository transactionRepository
    ) {
        this.mlFraudClient = mlFraudClient;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public MlFraudScore score(MlFraudScoringContext context) {
        MlFraudPredictionResponse prediction = mlFraudClient.predict(toRequest(context));
        boolean fallbackUsed = prediction.isUnavailable();
        return new MlFraudScore(
                prediction.fraudProbability()
                        .multiply(new BigDecimal("100.00"))
                        .setScale(2, RoundingMode.HALF_UP),
                fallbackUsed ? FALLBACK_VERSION : MODEL_VERSION,
                fallbackUsed,
                prediction.topReasons()
        );
    }

    private MlFraudPredictionRequest toRequest(MlFraudScoringContext context) {
        BigDecimal averageAmount = averageCustomerAmount(context);
        Long beneficiaryAgeHours = context.beneficiary() == null
                ? null
                : nonNegativeHours(Duration.between(
                        context.beneficiary().getAddedAt(), context.event().initiatedAt()));
        Long deviceAgeDays = context.device() == null
                ? null
                : Math.max(0L, Duration.between(
                        context.device().getFirstSeenAt(), context.event().initiatedAt()).toDays());

        return new MlFraudPredictionRequest(
                context.event().transactionReference(),
                context.event().amount(),
                averageAmount,
                context.event().currency(),
                context.event().transactionType(),
                context.event().channel(),
                context.event().initiatedAt().atOffset(java.time.ZoneOffset.UTC).getHour(),
                Math.max(0L, Duration.between(
                        context.account().getOpenedAt(), context.event().initiatedAt()).toDays()),
                context.velocityMetrics().transactionCount5Minutes(),
                context.velocityMetrics().totalAmount1Hour(),
                0,
                beneficiaryAgeHours,
                context.beneficiary() == null ? BigDecimal.ZERO : context.beneficiary().getRiskScore(),
                context.device() != null && context.device().isTrusted(),
                deviceAgeDays,
                ipAddressChanged(context),
                geoDistanceProxy(context.event().geoLocation(), context.customer()),
                context.customerRiskScore(),
                context.amlRiskScore()
        );
    }

    private BigDecimal averageCustomerAmount(MlFraudScoringContext context) {
        Double average = transactionRepository.findAverageAmountByCustomerIdExcludingTransaction(
                context.customer().getId(),
                context.transaction().getId(),
                context.event().initiatedAt()
        );
        return average == null
                ? context.event().amount()
                : BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean ipAddressChanged(MlFraudScoringContext context) {
        return context.device() != null
                && context.event().ipAddress() != null
                && !context.event().ipAddress().equals(context.device().getIpAddress());
    }

    private double geoDistanceProxy(String geoLocation, Customer customer) {
        if (geoLocation == null || geoLocation.isBlank()) {
            return 0.0;
        }
        String normalizedLocation = geoLocation.toLowerCase(Locale.ROOT);
        boolean matchesProfile = normalizedLocation.contains(customer.getCity().toLowerCase(Locale.ROOT))
                || normalizedLocation.contains(customer.getCountry().toLowerCase(Locale.ROOT));
        return matchesProfile ? 0.0 : 500.0;
    }

    private long nonNegativeHours(Duration duration) {
        return Math.max(0L, duration.toHours());
    }
}
