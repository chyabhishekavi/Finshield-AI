package com.finshield.backend.risk;

import com.finshield.backend.account.domain.Account;
import com.finshield.backend.audit.AuditService;
import com.finshield.backend.audit.domain.AuditAction;
import com.finshield.backend.aml.pattern.AmlDetectionResult;
import com.finshield.backend.aml.pattern.AmlDetectionService;
import com.finshield.backend.aml.AmlScreeningService;
import com.finshield.backend.aml.api.AmlScreeningResponse;
import com.finshield.backend.beneficiary.domain.Beneficiary;
import com.finshield.backend.beneficiary.repository.BeneficiaryRepository;
import com.finshield.backend.common.exception.ResourceNotFoundException;
import com.finshield.backend.customer.domain.Customer;
import com.finshield.backend.device.domain.CustomerDevice;
import com.finshield.backend.device.repository.CustomerDeviceRepository;
import com.finshield.backend.fraud.api.RuleEvaluationResult;
import com.finshield.backend.fraud.alert.FraudAlertService;
import com.finshield.backend.fraud.engine.FraudRuleEvaluationEngine;
import com.finshield.backend.risk.aml.AmlRiskScoreProvider;
import com.finshield.backend.risk.domain.TransactionRiskScore;
import com.finshield.backend.risk.ml.MlFraudScore;
import com.finshield.backend.risk.ml.MlFraudScoringContext;
import com.finshield.backend.risk.ml.MlFraudScoreProvider;
import com.finshield.backend.risk.mapper.TransactionRiskScoreMapper;
import com.finshield.backend.risk.repository.TransactionRiskScoreRepository;
import com.finshield.backend.risk.repository.TransactionRuleMatchRepository;
import com.finshield.backend.risk.velocity.VelocityDetectionService;
import com.finshield.backend.risk.velocity.VelocityMetrics;
import com.finshield.backend.transaction.domain.RiskBand;
import com.finshield.backend.transaction.domain.Transaction;
import com.finshield.backend.transaction.domain.TransactionChannel;
import com.finshield.backend.transaction.domain.TransactionDecision;
import com.finshield.backend.transaction.event.TransactionEvent;
import com.finshield.backend.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;

@Service
public class BaselineRiskScoringService implements RiskScoringService {

    private static final String SCORING_VERSION = "weighted-risk-v2-aml-patterns";
    private static final BigDecimal MAX_SCORE = new BigDecimal("100.00");
    private static final BigDecimal RULE_WEIGHT = new BigDecimal("0.45");
    private static final BigDecimal ML_WEIGHT = new BigDecimal("0.35");
    private static final BigDecimal CUSTOMER_WEIGHT = new BigDecimal("0.10");
    private static final BigDecimal DEVICE_WEIGHT = new BigDecimal("0.05");
    private static final BigDecimal AML_WEIGHT = new BigDecimal("0.05");

    private final TransactionRepository transactionRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final CustomerDeviceRepository deviceRepository;
    private final FraudRuleEvaluationEngine ruleEvaluationEngine;
    private final VelocityDetectionService velocityDetectionService;
    private final MlFraudScoreProvider mlFraudScoreProvider;
    private final AmlRiskScoreProvider amlRiskScoreProvider;
    private final AmlDetectionService amlDetectionService;
    private final AmlScreeningService amlScreeningService;
    private final TransactionRiskScoreRepository riskScoreRepository;
    private final TransactionRuleMatchRepository ruleMatchRepository;
    private final TransactionRiskScoreMapper riskScoreMapper;
    private final FraudAlertService fraudAlertService;
    private final AuditService auditService;

    public BaselineRiskScoringService(
            TransactionRepository transactionRepository,
            BeneficiaryRepository beneficiaryRepository,
            CustomerDeviceRepository deviceRepository,
            FraudRuleEvaluationEngine ruleEvaluationEngine,
            VelocityDetectionService velocityDetectionService,
            MlFraudScoreProvider mlFraudScoreProvider,
            AmlRiskScoreProvider amlRiskScoreProvider,
            AmlDetectionService amlDetectionService,
            AmlScreeningService amlScreeningService,
            TransactionRiskScoreRepository riskScoreRepository,
            TransactionRuleMatchRepository ruleMatchRepository,
            TransactionRiskScoreMapper riskScoreMapper,
            FraudAlertService fraudAlertService,
            AuditService auditService
    ) {
        this.transactionRepository = transactionRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.deviceRepository = deviceRepository;
        this.ruleEvaluationEngine = ruleEvaluationEngine;
        this.velocityDetectionService = velocityDetectionService;
        this.mlFraudScoreProvider = mlFraudScoreProvider;
        this.amlRiskScoreProvider = amlRiskScoreProvider;
        this.amlDetectionService = amlDetectionService;
        this.amlScreeningService = amlScreeningService;
        this.riskScoreRepository = riskScoreRepository;
        this.ruleMatchRepository = ruleMatchRepository;
        this.riskScoreMapper = riskScoreMapper;
        this.fraudAlertService = fraudAlertService;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public RiskScoringResult score(TransactionEvent event) {
        Transaction transaction = transactionRepository.findById(event.transactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", event.transactionId()));
        if (!transaction.getTransactionReference().equals(event.transactionReference())) {
            throw new IllegalArgumentException("Transaction event reference does not match the stored transaction");
        }
        validateImmutableFacts(transaction, event);

        Account account = transaction.getSourceAccount();
        Customer customer = account.getCustomer();
        VelocityMetrics velocityMetrics = velocityDetectionService.recordTransaction(
                event.eventId(),
                customer.getId(),
                event.destinationAccountNumber(),
                event.amount(),
                event.deviceId(),
                event.initiatedAt()
        );

        Optional<TransactionRiskScore> existingScore = riskScoreRepository.findBySourceEventId(event.eventId());
        if (existingScore.isPresent()) {
            recordSuspiciousDeviceAttempt(event, existingScore.get().getDecision());
            return existingResult(existingScore.get());
        }

        Optional<Beneficiary> beneficiary = beneficiaryRepository
                .findByCustomerIdAndBeneficiaryAccountNumber(
                        customer.getId(), event.destinationAccountNumber());
        Optional<CustomerDevice> device = event.deviceId() == null
                ? Optional.empty()
                : deviceRepository.findByCustomerIdAndDeviceId(customer.getId(), event.deviceId());

        RuleEvaluationResult ruleResult = ruleEvaluationEngine.evaluate(
                transaction,
                customer,
                account,
                device.orElse(null),
                beneficiary.orElse(null),
                velocityMetrics
        );
        BigDecimal ruleScore = normalizeScore(ruleResult.totalRuleScore());
        if (!ruleResult.evaluationErrors().isEmpty()) {
            ruleScore = ruleScore.max(new BigDecimal("80.00"));
        }
        BigDecimal customerRiskScore = customerRiskScore(customer);
        BigDecimal deviceRiskScore = deviceRiskScore(event, device);
        AmlDetectionResult amlDetection = amlDetectionService.detect(transaction);
        BigDecimal profileAmlScore = normalizeScore(amlRiskScoreProvider.score(customer));
        BigDecimal watchlistScore = beneficiary
                .map(value -> watchlistRiskScore(amlScreeningService.screenBeneficiary(value.getId())))
                .orElse(BigDecimal.ZERO.setScale(2));
        BigDecimal amlRiskScore = profileAmlScore
                .max(normalizeScore(amlDetection.amlScore()))
                .max(watchlistScore);
        List<String> amlExplanations = new ArrayList<>();
        amlExplanations.add("Profile/KYC baseline score " + profileAmlScore);
        if (watchlistScore.signum() > 0) {
            amlExplanations.add("Beneficiary watchlist screening score " + watchlistScore);
        }
        if (amlDetection.matchedPatterns().isEmpty()) {
            amlExplanations.add(amlDetection.explanation());
        } else {
            amlExplanations.addAll(amlDetection.matchedPatterns().stream()
                    .map(match -> match.pattern() + ": " + match.explanation())
                    .toList());
        }
        MlFraudScore mlScore = mlFraudScoreProvider.score(new MlFraudScoringContext(
                event,
                transaction,
                customer,
                account,
                device.orElse(null),
                beneficiary.orElse(null),
                velocityMetrics,
                ruleResult,
                customerRiskScore,
                amlRiskScore
        ));
        BigDecimal normalizedMlScore = normalizeScore(mlScore.score());

        BigDecimal finalScore = weightedScore(
                ruleScore,
                normalizedMlScore,
                customerRiskScore,
                deviceRiskScore,
                amlRiskScore
        );
        RiskBand band = riskBand(finalScore);
        TransactionDecision decision = decision(band);
        Instant scoredAt = Instant.now();
        Map<String, Object> oldDecision = Map.of("riskScore", transaction.getRiskScore(),
                "riskBand", transaction.getRiskBand(), "decision", transaction.getDecision());
        transaction.applyRiskDecision(finalScore, band, decision);
        transactionRepository.saveAndFlush(transaction);
        auditService.log(AuditAction.TRANSACTION_DECISION_CHANGED, "Transaction", transaction.getId(),
                oldDecision, Map.of("riskScore", finalScore, "riskBand", band, "decision", decision));

        TransactionRiskScore persistedScore = riskScoreRepository.saveAndFlush(riskScoreMapper.toRiskScore(
                event.eventId(),
                transaction,
                ruleResult,
                ruleScore,
                normalizedMlScore,
                customerRiskScore,
                deviceRiskScore,
                amlRiskScore,
                finalScore,
                band,
                decision,
                SCORING_VERSION,
                mlScore.modelVersion(),
                mlScore.fallbackUsed(),
                mlScore.explanations(),
                amlExplanations,
                scoredAt
        ));
        ruleMatchRepository.saveAll(riskScoreMapper.toRuleMatches(
                persistedScore, transaction, ruleResult));
        if (decision == TransactionDecision.CREATE_ALERT
                || decision == TransactionDecision.HOLD_AND_ESCALATE) {
            fraudAlertService.createFromRiskScore(persistedScore);
        }
        recordSuspiciousDeviceAttempt(event, decision);

        return existingResult(persistedScore);
    }

    private BigDecimal weightedScore(
            BigDecimal ruleScore,
            BigDecimal mlScore,
            BigDecimal customerScore,
            BigDecimal deviceScore,
            BigDecimal amlScore
    ) {
        return ruleScore.multiply(RULE_WEIGHT)
                .add(mlScore.multiply(ML_WEIGHT))
                .add(customerScore.multiply(CUSTOMER_WEIGHT))
                .add(deviceScore.multiply(DEVICE_WEIGHT))
                .add(amlScore.multiply(AML_WEIGHT))
                .min(MAX_SCORE)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private RiskBand riskBand(BigDecimal score) {
        if (score.compareTo(new BigDecimal("30.00")) <= 0) {
            return RiskBand.LOW;
        }
        if (score.compareTo(new BigDecimal("60.00")) <= 0) {
            return RiskBand.MEDIUM;
        }
        if (score.compareTo(new BigDecimal("80.00")) <= 0) {
            return RiskBand.HIGH;
        }
        return RiskBand.CRITICAL;
    }

    private TransactionDecision decision(RiskBand band) {
        return switch (band) {
            case LOW -> TransactionDecision.APPROVE;
            case MEDIUM -> TransactionDecision.MONITOR;
            case HIGH -> TransactionDecision.CREATE_ALERT;
            case CRITICAL -> TransactionDecision.HOLD_AND_ESCALATE;
        };
    }

    private RiskScoringResult existingResult(TransactionRiskScore score) {
        return new RiskScoringResult(
                score.getId(),
                score.getRuleScore(),
                score.getMlScore(),
                score.getCustomerRiskScore(),
                score.getDeviceRiskScore(),
                score.getAmlScore(),
                score.getFinalScore(),
                score.getRiskBand(),
                score.getDecision(),
                score.getScoringVersion(),
                score.getMlModelVersion(),
                score.isMlFallbackUsed(),
                score.getScoredAt()
        );
    }

    private BigDecimal customerRiskScore(Customer customer) {
        return switch (customer.getCustomerRiskLevel()) {
            case LOW -> new BigDecimal("10.00");
            case MEDIUM -> new BigDecimal("40.00");
            case HIGH -> new BigDecimal("75.00");
            case CRITICAL -> new BigDecimal("100.00");
        };
    }

    private BigDecimal watchlistRiskScore(AmlScreeningResponse screening) {
        return screening.results().stream()
                .filter(result -> result.riskCategory() != null)
                .map(result -> switch (result.riskCategory()) {
                    case LOW -> new BigDecimal("25.00");
                    case MEDIUM -> new BigDecimal("50.00");
                    case HIGH -> new BigDecimal("75.00");
                    case CRITICAL -> new BigDecimal("100.00");
                })
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO)
                .setScale(2);
    }

    private BigDecimal deviceRiskScore(
            TransactionEvent event,
            Optional<CustomerDevice> device
    ) {
        boolean digitalChannel = event.channel() == TransactionChannel.MOBILE_BANKING
                || event.channel() == TransactionChannel.INTERNET_BANKING
                || event.channel() == TransactionChannel.API;
        if (device.isEmpty()) {
            return digitalChannel ? new BigDecimal("100.00") : new BigDecimal("20.00");
        }
        CustomerDevice knownDevice = device.get();
        if (!knownDevice.isTrusted()) {
            return new BigDecimal("70.00");
        }
        if (event.ipAddress() != null && !event.ipAddress().equals(knownDevice.getIpAddress())) {
            return new BigDecimal("40.00");
        }
        return new BigDecimal("5.00");
    }

    private BigDecimal normalizeScore(BigDecimal score) {
        if (score == null || score.signum() < 0) {
            throw new IllegalArgumentException("Risk component score must be between 0 and 100");
        }
        return score.min(MAX_SCORE).setScale(2, RoundingMode.HALF_UP);
    }

    private void validateImmutableFacts(Transaction transaction, TransactionEvent event) {
        boolean mismatch = !transaction.getSourceAccount().getId().equals(event.sourceAccountId())
                || !transaction.getDestinationAccountNumber().equals(event.destinationAccountNumber())
                || transaction.getAmount().compareTo(event.amount()) != 0
                || !transaction.getCurrency().equals(event.currency())
                || transaction.getTransactionType() != event.transactionType()
                || transaction.getChannel() != event.channel()
                // PostgreSQL stores timestamps at microsecond precision while API Instants may carry nanoseconds.
                || Math.abs(ChronoUnit.NANOS.between(
                        transaction.getInitiatedAt(), event.initiatedAt())) > 1_000;
        if (mismatch) {
            throw new IllegalArgumentException("Transaction event does not match immutable stored transaction data");
        }
    }

    private void recordSuspiciousDeviceAttempt(
            TransactionEvent event,
            TransactionDecision decision
    ) {
        if (event.deviceId() != null
                && !event.deviceId().isBlank()
                && decision != TransactionDecision.APPROVE
                && decision != TransactionDecision.PENDING) {
            velocityDetectionService.recordFailedOrSuspiciousAttempt(
                    event.eventId(), event.deviceId(), event.initiatedAt());
        }
    }
}
