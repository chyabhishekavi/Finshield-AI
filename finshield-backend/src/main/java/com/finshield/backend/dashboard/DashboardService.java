package com.finshield.backend.dashboard;

import com.finshield.backend.casework.domain.CasePriority;
import com.finshield.backend.casework.domain.InvestigationCase;
import com.finshield.backend.casework.domain.InvestigationCaseStatus;
import com.finshield.backend.casework.repository.InvestigationCaseRepository;
import com.finshield.backend.dashboard.api.*;
import com.finshield.backend.fraud.alert.domain.FraudAlertSeverity;
import com.finshield.backend.fraud.alert.domain.FraudAlertStatus;
import com.finshield.backend.fraud.alert.repository.FraudAlertRepository;
import com.finshield.backend.risk.domain.TransactionRiskScore;
import com.finshield.backend.risk.repository.TransactionRiskScoreRepository;
import com.finshield.backend.risk.repository.TransactionRuleMatchRepository;
import com.finshield.backend.transaction.api.TransactionResponse;
import com.finshield.backend.transaction.domain.RiskBand;
import com.finshield.backend.transaction.repository.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

@Service
public class DashboardService {
    private static final Set<RiskBand> HIGH_RISK_BANDS = Set.of(RiskBand.HIGH, RiskBand.CRITICAL);
    private static final Set<FraudAlertStatus> OPEN_ALERT_STATUSES = Set.of(
            FraudAlertStatus.NEW, FraudAlertStatus.ASSIGNED,
            FraudAlertStatus.IN_REVIEW, FraudAlertStatus.ESCALATED);

    private final TransactionRepository transactionRepository;
    private final TransactionRiskScoreRepository riskScoreRepository;
    private final TransactionRuleMatchRepository ruleMatchRepository;
    private final FraudAlertRepository alertRepository;
    private final InvestigationCaseRepository caseRepository;

    public DashboardService(TransactionRepository transactionRepository,
            TransactionRiskScoreRepository riskScoreRepository,
            TransactionRuleMatchRepository ruleMatchRepository,
            FraudAlertRepository alertRepository,
            InvestigationCaseRepository caseRepository) {
        this.transactionRepository = transactionRepository;
        this.riskScoreRepository = riskScoreRepository;
        this.ruleMatchRepository = ruleMatchRepository;
        this.alertRepository = alertRepository;
        this.caseRepository = caseRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary() {
        return new DashboardSummaryResponse(transactionRepository.count(),
                transactionRepository.countByRiskBandIn(HIGH_RISK_BANDS), alertRepository.count(),
                alertRepository.countByStatusIn(OPEN_ALERT_STATUSES),
                alertRepository.countBySeverity(FraudAlertSeverity.CRITICAL), caseRepository.count(),
                caseRepository.countByStatusNot(InvestigationCaseStatus.CLOSED), Instant.now());
    }

    @Transactional(readOnly = true)
    public RiskTrendResponse riskTrends(int days) {
        Instant now = Instant.now();
        LocalDate endDate = LocalDate.ofInstant(now, ZoneOffset.UTC);
        LocalDate startDate = endDate.minusDays(days - 1L);
        Instant from = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        LinkedHashMap<LocalDate, RiskAccumulator> buckets = new LinkedHashMap<>();
        startDate.datesUntil(endDate.plusDays(1)).forEach(date -> buckets.put(date, new RiskAccumulator()));
        for (TransactionRiskScore score : riskScoreRepository.findAllByScoredAtBetweenOrderByScoredAtAsc(from, now)) {
            RiskAccumulator bucket = buckets.get(LocalDate.ofInstant(score.getScoredAt(), ZoneOffset.UTC));
            if (bucket != null) bucket.add(score);
        }
        List<RiskTrendResponse.RiskTrendPoint> series = buckets.entrySet().stream()
                .map(entry -> entry.getValue().toPoint(entry.getKey())).toList();
        return new RiskTrendResponse(from, now, "DAY", series);
    }

    @Transactional(readOnly = true)
    public TopRulesResponse topRules(int days, int limit) {
        Instant from = Instant.now().minus(Duration.ofDays(days));
        List<TopRulesResponse.TopRuleItem> rules = ruleMatchRepository
                .findTopRules(from, PageRequest.of(0, limit)).stream()
                .map(value -> new TopRulesResponse.TopRuleItem(value.getRuleCode(), value.getRuleName(),
                        value.getMatchCount(), value.getTotalScoreImpact())).toList();
        return new TopRulesResponse(from, rules);
    }

    @Transactional(readOnly = true)
    public AlertStatusCountResponse alertStatusCounts() {
        EnumMap<FraudAlertStatus, Long> counts = new EnumMap<>(FraudAlertStatus.class);
        Arrays.stream(FraudAlertStatus.values()).forEach(status -> counts.put(status, 0L));
        alertRepository.countByStatusGrouped().forEach(value -> counts.put(value.getStatus(), value.getAlertCount()));
        return new AlertStatusCountResponse(counts.entrySet().stream()
                .map(entry -> new AlertStatusCountResponse.StatusCount(entry.getKey().name(), entry.getValue()))
                .toList());
    }

    @Transactional(readOnly = true)
    public HighRiskTransactionsResponse highRiskTransactions(int limit) {
        long total = transactionRepository.countByRiskBandIn(HIGH_RISK_BANDS);
        List<TransactionResponse> transactions = transactionRepository
                .findAllByRiskBandInOrderByInitiatedAtDesc(HIGH_RISK_BANDS, PageRequest.of(0, limit))
                .stream().map(TransactionResponse::from).toList();
        return new HighRiskTransactionsResponse(total, transactions);
    }

    @Transactional(readOnly = true)
    public CaseSlaSummaryResponse caseSlaSummary(int days) {
        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofDays(days));
        EnumMap<CasePriority, SlaAccumulator> buckets = new EnumMap<>(CasePriority.class);
        Arrays.stream(CasePriority.values()).forEach(priority -> buckets.put(priority, new SlaAccumulator()));
        long resolutionSeconds = 0;
        long resolvedCount = 0;
        for (InvestigationCase value : caseRepository.findForSlaSummary(
                from, now, InvestigationCaseStatus.CLOSED)) {
            Instant deadline = value.getCreatedAt().plus(sla(value.getPriority()));
            SlaAccumulator bucket = buckets.get(value.getPriority());
            if (value.getStatus() != InvestigationCaseStatus.CLOSED) {
                bucket.open++;
                if (now.isAfter(deadline)) bucket.overdue++;
                else if (!now.isBefore(deadline.minus(dueSoonWindow(value.getPriority())))) bucket.dueSoon++;
            } else if (value.getClosedAt() != null) {
                if (value.getClosedAt().isAfter(deadline)) bucket.closedAfter++;
                else bucket.closedWithin++;
                resolutionSeconds += Duration.between(value.getCreatedAt(), value.getClosedAt()).toSeconds();
                resolvedCount++;
            }
        }
        List<CaseSlaSummaryResponse.PrioritySlaBucket> series = buckets.entrySet().stream()
                .map(entry -> entry.getValue().toResponse(entry.getKey())).toList();
        BigDecimal averageHours = resolvedCount == 0 ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(resolutionSeconds)
                        .divide(BigDecimal.valueOf(resolvedCount * 3600L), 2, RoundingMode.HALF_UP);
        return new CaseSlaSummaryResponse(from, now,
                buckets.values().stream().mapToLong(value -> value.open).sum(),
                buckets.values().stream().mapToLong(value -> value.overdue).sum(),
                buckets.values().stream().mapToLong(value -> value.dueSoon).sum(),
                buckets.values().stream().mapToLong(value -> value.closedWithin).sum(),
                buckets.values().stream().mapToLong(value -> value.closedAfter).sum(),
                averageHours, series);
    }

    private Duration sla(CasePriority priority) {
        return switch (priority) {
            case LOW -> Duration.ofDays(7);
            case MEDIUM -> Duration.ofDays(3);
            case HIGH -> Duration.ofHours(24);
            case CRITICAL -> Duration.ofHours(4);
        };
    }

    private Duration dueSoonWindow(CasePriority priority) {
        return sla(priority).dividedBy(4);
    }

    private static final class RiskAccumulator {
        private long total, low, medium, high, critical;
        private BigDecimal scoreTotal = BigDecimal.ZERO;
        void add(TransactionRiskScore score) {
            total++;
            scoreTotal = scoreTotal.add(score.getFinalScore());
            switch (score.getRiskBand()) {
                case LOW -> low++;
                case MEDIUM -> medium++;
                case HIGH -> high++;
                case CRITICAL -> critical++;
            }
        }
        RiskTrendResponse.RiskTrendPoint toPoint(LocalDate date) {
            BigDecimal average = total == 0 ? BigDecimal.ZERO.setScale(2)
                    : scoreTotal.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
            return new RiskTrendResponse.RiskTrendPoint(date, total, low, medium, high, critical, average);
        }
    }

    private static final class SlaAccumulator {
        private long open, overdue, dueSoon, closedWithin, closedAfter;
        CaseSlaSummaryResponse.PrioritySlaBucket toResponse(CasePriority priority) {
            return new CaseSlaSummaryResponse.PrioritySlaBucket(priority.name(), open, overdue,
                    dueSoon, closedWithin, closedAfter);
        }
    }
}
