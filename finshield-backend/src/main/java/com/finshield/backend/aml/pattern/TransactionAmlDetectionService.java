package com.finshield.backend.aml.pattern;

import com.finshield.backend.account.domain.Account;
import com.finshield.backend.transaction.domain.Transaction;
import com.finshield.backend.transaction.domain.TransactionStatus;
import com.finshield.backend.transaction.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class TransactionAmlDetectionService implements AmlDetectionService {

    private static final BigDecimal MAX_SCORE = new BigDecimal("100.00");
    private static final Set<TransactionStatus> EXCLUDED_STATUSES = Set.of(
            TransactionStatus.DECLINED,
            TransactionStatus.FAILED,
            TransactionStatus.REVERSED,
            TransactionStatus.CANCELLED
    );

    private final TransactionRepository transactionRepository;
    private final BigDecimal structuringThreshold;
    private final int structuringMinimumCount;
    private final int muleMinimumIncoming;
    private final int muleMinimumOutgoing;
    private final BigDecimal muleOutflowRatio;
    private final Duration rapidMovementWindow;
    private final BigDecimal rapidMovementRatio;
    private final BigDecimal roundAmountUnit;
    private final int roundAmountMinimumCount;
    private final Set<String> highRiskCountries;

    public TransactionAmlDetectionService(
            TransactionRepository transactionRepository,
            @Value("${finshield.aml.patterns.structuring-threshold:10000}") BigDecimal structuringThreshold,
            @Value("${finshield.aml.patterns.structuring-minimum-count:3}") int structuringMinimumCount,
            @Value("${finshield.aml.patterns.mule-minimum-incoming:3}") int muleMinimumIncoming,
            @Value("${finshield.aml.patterns.mule-minimum-outgoing:2}") int muleMinimumOutgoing,
            @Value("${finshield.aml.patterns.mule-outflow-ratio:0.70}") BigDecimal muleOutflowRatio,
            @Value("${finshield.aml.patterns.rapid-movement-window:30m}") Duration rapidMovementWindow,
            @Value("${finshield.aml.patterns.rapid-movement-ratio:0.50}") BigDecimal rapidMovementRatio,
            @Value("${finshield.aml.patterns.round-amount-unit:1000}") BigDecimal roundAmountUnit,
            @Value("${finshield.aml.patterns.round-amount-minimum-count:3}") int roundAmountMinimumCount,
            @Value("${finshield.aml.patterns.high-risk-countries:IRAN,NORTH KOREA,SYRIA,MYANMAR}")
            String highRiskCountries
    ) {
        this.transactionRepository = transactionRepository;
        this.structuringThreshold = requirePositive(structuringThreshold, "structuring threshold");
        this.structuringMinimumCount = requireAtLeast(structuringMinimumCount, 2, "structuring minimum count");
        this.muleMinimumIncoming = requireAtLeast(muleMinimumIncoming, 1, "mule minimum incoming");
        this.muleMinimumOutgoing = requireAtLeast(muleMinimumOutgoing, 1, "mule minimum outgoing");
        this.muleOutflowRatio = requireRatio(muleOutflowRatio, "mule outflow ratio");
        if (rapidMovementWindow.isZero() || rapidMovementWindow.isNegative()) {
            throw new IllegalArgumentException("rapid movement window must be positive");
        }
        this.rapidMovementWindow = rapidMovementWindow;
        this.rapidMovementRatio = requireRatio(rapidMovementRatio, "rapid movement ratio");
        this.roundAmountUnit = requirePositive(roundAmountUnit, "round amount unit");
        this.roundAmountMinimumCount = requireAtLeast(roundAmountMinimumCount, 2,
                "round amount minimum count");
        this.highRiskCountries = Arrays.stream(highRiskCountries.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    @Transactional(readOnly = true)
    public AmlDetectionResult detect(Transaction transaction) {
        Account account = transaction.getSourceAccount();
        Instant evaluationTime = transaction.getInitiatedAt();
        List<Transaction> activity = transactionRepository.findAccountActivity(
                account.getId(),
                account.getAccountNumber(),
                evaluationTime.minus(Duration.ofHours(24)),
                evaluationTime,
                EXCLUDED_STATUSES
        );
        List<Transaction> incoming = activity.stream()
                .filter(item -> isIncoming(item, account))
                .toList();
        List<Transaction> outgoing = activity.stream()
                .filter(item -> item.getSourceAccount().getId().equals(account.getId()))
                .toList();

        List<AmlPatternMatch> matches = new ArrayList<>();
        detectStructuring(outgoing, matches);
        detectMuleBehavior(incoming, outgoing, matches);
        detectRapidMovement(incoming, outgoing, evaluationTime, matches);
        detectHighRiskCountry(transaction, matches);
        detectRoundAmounts(outgoing, matches);

        BigDecimal score = matches.stream()
                .map(AmlPatternMatch::scoreImpact)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .min(MAX_SCORE)
                .setScale(2, RoundingMode.HALF_UP);
        String explanation = matches.isEmpty()
                ? "No AML transaction patterns matched"
                : matches.stream().map(AmlPatternMatch::explanation)
                .collect(java.util.stream.Collectors.joining("; "));
        return new AmlDetectionResult(score, matches, explanation);
    }

    private void detectStructuring(List<Transaction> outgoing, List<AmlPatternMatch> matches) {
        List<Transaction> belowThreshold = outgoing.stream()
                .filter(item -> item.getAmount().compareTo(structuringThreshold) < 0)
                .toList();
        if (belowThreshold.size() >= structuringMinimumCount) {
            BigDecimal total = sum(belowThreshold);
            matches.add(new AmlPatternMatch(AmlPattern.STRUCTURING, new BigDecimal("30.00"),
                    "%d transactions below %s within 24 hours totalled %s".formatted(
                            belowThreshold.size(), structuringThreshold, total)));
        }
    }

    private void detectMuleBehavior(
            List<Transaction> incoming,
            List<Transaction> outgoing,
            List<AmlPatternMatch> matches
    ) {
        if (incoming.size() < muleMinimumIncoming || outgoing.size() < muleMinimumOutgoing) {
            return;
        }
        Instant firstIncoming = incoming.get(0).getInitiatedAt();
        List<Transaction> subsequentOutgoing = outgoing.stream()
                .filter(item -> item.getInitiatedAt().isAfter(firstIncoming))
                .toList();
        BigDecimal incomingTotal = sum(incoming);
        BigDecimal outgoingTotal = sum(subsequentOutgoing);
        if (subsequentOutgoing.size() >= muleMinimumOutgoing
                && ratio(outgoingTotal, incomingTotal).compareTo(muleOutflowRatio) >= 0) {
            matches.add(new AmlPatternMatch(AmlPattern.MULE_ACCOUNT_BEHAVIOR,
                    new BigDecimal("35.00"),
                    "%d incoming transfers were followed by %d outgoing transfers; outflow ratio was %s".formatted(
                            incoming.size(), subsequentOutgoing.size(), ratio(outgoingTotal, incomingTotal))));
        }
    }

    private void detectRapidMovement(
            List<Transaction> incoming,
            List<Transaction> outgoing,
            Instant evaluationTime,
            List<AmlPatternMatch> matches
    ) {
        Instant cutoff = evaluationTime.minus(rapidMovementWindow);
        List<Transaction> recentIncoming = incoming.stream()
                .filter(item -> !item.getInitiatedAt().isBefore(cutoff))
                .toList();
        if (recentIncoming.isEmpty()) {
            return;
        }
        Instant firstReceipt = recentIncoming.get(0).getInitiatedAt();
        List<Transaction> recentOutgoing = outgoing.stream()
                .filter(item -> !item.getInitiatedAt().isBefore(cutoff))
                .filter(item -> item.getInitiatedAt().isAfter(firstReceipt))
                .toList();
        BigDecimal incomingTotal = sum(recentIncoming);
        BigDecimal outgoingTotal = sum(recentOutgoing);
        if (!recentOutgoing.isEmpty()
                && ratio(outgoingTotal, incomingTotal).compareTo(rapidMovementRatio) >= 0) {
            matches.add(new AmlPatternMatch(AmlPattern.RAPID_MOVEMENT, new BigDecimal("30.00"),
                    "Funds received and sent within %d minutes; outgoing-to-incoming ratio was %s".formatted(
                            rapidMovementWindow.toMinutes(), ratio(outgoingTotal, incomingTotal))));
        }
    }

    private void detectHighRiskCountry(Transaction transaction, List<AmlPatternMatch> matches) {
        if (transaction.getGeoLocation() == null) {
            return;
        }
        String location = transaction.getGeoLocation().toUpperCase(Locale.ROOT);
        highRiskCountries.stream()
                .filter(location::contains)
                .findFirst()
                .ifPresent(country -> matches.add(new AmlPatternMatch(
                        AmlPattern.HIGH_RISK_COUNTRY,
                        new BigDecimal("35.00"),
                        "Transaction geo-location matched configured high-risk country: " + country
                )));
    }

    private void detectRoundAmounts(List<Transaction> outgoing, List<AmlPatternMatch> matches) {
        List<Transaction> roundAmounts = outgoing.stream()
                .filter(item -> item.getAmount().remainder(roundAmountUnit).compareTo(BigDecimal.ZERO) == 0)
                .toList();
        if (roundAmounts.size() >= roundAmountMinimumCount) {
            matches.add(new AmlPatternMatch(AmlPattern.REPEATED_ROUND_AMOUNTS,
                    new BigDecimal("20.00"),
                    "%d transactions were exact multiples of %s within 24 hours".formatted(
                            roundAmounts.size(), roundAmountUnit)));
        }
    }

    private boolean isIncoming(Transaction transaction, Account account) {
        return transaction.getDestinationAccountNumber().equals(account.getAccountNumber())
                && !transaction.getSourceAccount().getId().equals(account.getId());
    }

    private BigDecimal sum(List<Transaction> transactions) {
        return transactions.stream().map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        return denominator.signum() == 0
                ? BigDecimal.ZERO.setScale(2)
                : numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal requirePositive(BigDecimal value, String name) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static BigDecimal requireRatio(BigDecimal value, String name) {
        if (value == null || value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(name + " must be between 0 and 1");
        }
        return value;
    }

    private static int requireAtLeast(int value, int minimum, String name) {
        if (value < minimum) {
            throw new IllegalArgumentException(name + " must be at least " + minimum);
        }
        return value;
    }
}
