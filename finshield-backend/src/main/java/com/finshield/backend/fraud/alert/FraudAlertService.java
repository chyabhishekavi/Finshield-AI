package com.finshield.backend.fraud.alert;

import com.finshield.backend.common.api.PageResponse;
import com.finshield.backend.audit.AuditService;
import com.finshield.backend.audit.domain.AuditAction;
import com.finshield.backend.casework.InvestigationCaseService;
import com.finshield.backend.common.exception.BadRequestException;
import com.finshield.backend.common.exception.ResourceNotFoundException;
import com.finshield.backend.fraud.alert.api.AssignFraudAlertRequest;
import com.finshield.backend.fraud.alert.api.CloseFraudAlertRequest;
import com.finshield.backend.fraud.alert.api.EscalateFraudAlertRequest;
import com.finshield.backend.fraud.alert.api.FraudAlertResponse;
import com.finshield.backend.fraud.alert.api.UpdateFraudAlertStatusRequest;
import com.finshield.backend.fraud.alert.api.AlertAssigneeResponse;
import com.finshield.backend.fraud.alert.domain.FraudAlert;
import com.finshield.backend.fraud.alert.domain.FraudAlertSeverity;
import com.finshield.backend.fraud.alert.domain.FraudAlertStatus;
import com.finshield.backend.fraud.alert.repository.FraudAlertRepository;
import com.finshield.backend.fraud.alert.event.FraudAlertEventOutbox;
import com.finshield.backend.fraud.alert.event.FraudAlertEventOutboxRepository;
import com.finshield.backend.notification.NotificationService;
import com.finshield.backend.risk.domain.TransactionRiskScore;
import com.finshield.backend.transaction.domain.RiskBand;
import com.finshield.backend.transaction.domain.Transaction;
import com.finshield.backend.transaction.domain.TransactionDecision;
import com.finshield.backend.user.domain.RoleName;
import com.finshield.backend.user.domain.User;
import com.finshield.backend.user.domain.UserStatus;
import com.finshield.backend.user.repository.UserRepository;
import com.finshield.backend.user.repository.UserRoleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.UUID;

@Service
public class FraudAlertService {

    private static final DateTimeFormatter ALERT_DATE = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneOffset.UTC);
    private static final Set<FraudAlertStatus> TERMINAL_STATUSES = Set.of(
            FraudAlertStatus.CLOSED_FRAUD,
            FraudAlertStatus.CLOSED_FALSE_POSITIVE,
            FraudAlertStatus.CONVERTED_TO_CASE
    );

    private final FraudAlertRepository alertRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final NotificationService notificationService;
    private final InvestigationCaseService investigationCaseService;
    private final FraudAlertEventOutboxRepository outboxRepository;
    private final boolean autoCreateCriticalCase;
    private final AuditService auditService;

    public FraudAlertService(
            FraudAlertRepository alertRepository,
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            NotificationService notificationService,
            InvestigationCaseService investigationCaseService,
            FraudAlertEventOutboxRepository outboxRepository,
            AuditService auditService,
            @Value("${finshield.alerts.auto-create-critical-case:false}") boolean autoCreateCriticalCase
    ) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.notificationService = notificationService;
        this.investigationCaseService = investigationCaseService;
        this.outboxRepository = outboxRepository;
        this.autoCreateCriticalCase = autoCreateCriticalCase;
        this.auditService = auditService;
    }

    @Transactional
    public FraudAlert createFromRiskScore(TransactionRiskScore riskScore) {
        Transaction transaction = riskScore.getTransaction();
        if (riskScore.getDecision() != TransactionDecision.CREATE_ALERT
                && riskScore.getDecision() != TransactionDecision.HOLD_AND_ESCALATE) {
            throw new IllegalArgumentException("Risk decision does not require a fraud alert");
        }
        FraudAlert existing = alertRepository.findByTransactionId(transaction.getId()).orElse(null);
        if (existing != null) {
            return existing;
        }
        FraudAlert alert = new FraudAlert(
                nextAlertNumber(),
                transaction,
                transaction.getSourceAccount().getCustomer(),
                riskScore.getFinalScore(),
                riskScore.getRiskBand(),
                severity(riskScore.getRiskBand()),
                riskScore.getScoredAt().plus(sla(riskScore.getRiskBand())),
                initialSummary(riskScore)
        );
        if (riskScore.getDecision() == TransactionDecision.HOLD_AND_ESCALATE) {
            alert.escalate("Critical risk decision automatically held and escalated", null);
        }
        alert = alertRepository.saveAndFlush(alert);
        notificationService.notifyAlertCreated(alert);
        if (riskScore.getRiskBand() == RiskBand.CRITICAL) {
            notificationService.createUrgentFraudAlertNotification(alert);
            if (autoCreateCriticalCase) {
                investigationCaseService.createAutomaticCriticalCase(alert);
            }
        }
        outboxRepository.save(new FraudAlertEventOutbox(UUID.randomUUID(), alert));
        return alert;
    }

    @Transactional(readOnly = true)
    public PageResponse<FraudAlertResponse> search(
            String query,
            FraudAlertStatus status,
            FraudAlertSeverity severity,
            RiskBand riskBand,
            UUID assignedToId,
            UUID customerId,
            Boolean overdue,
            String sortBy,
            Sort.Direction sortDirection,
            int page,
            int size
    ) {
        // Bind an empty varchar instead of null so PostgreSQL does not infer the LOWER parameter as bytea.
        String normalizedQuery = query == null || query.isBlank() ? "" : query.trim();
        Instant now = Instant.now();
        if (!Set.of("riskScore", "dueAt", "createdAt").contains(sortBy)) {
            throw new BadRequestException("sortBy must be riskScore, dueAt, or createdAt");
        }
        return PageResponse.from(alertRepository.search(
                normalizedQuery,
                status,
                severity,
                riskBand,
                assignedToId,
                customerId,
                overdue,
                now,
                TERMINAL_STATUSES,
                PageRequest.of(page, size, Sort.by(sortDirection, sortBy))
        ), alert -> FraudAlertResponse.from(alert, now));
    }

    @Transactional(readOnly = true)
    public List<AlertAssigneeResponse> eligibleAssignees() {
        LinkedHashMap<UUID, User> users = new LinkedHashMap<>();
        Set.of(RoleName.ADMIN, RoleName.FRAUD_ANALYST, RoleName.RISK_MANAGER).forEach(role ->
                userRoleRepository.findUsersByRoleAndStatus(role, UserStatus.ACTIVE)
                        .forEach(user -> users.put(user.getId(), user)));
        return users.values().stream().map(AlertAssigneeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public FraudAlertResponse getById(UUID alertId) {
        return FraudAlertResponse.from(findAlert(alertId), Instant.now());
    }

    @Transactional
    public FraudAlertResponse assign(UUID alertId, AssignFraudAlertRequest request) {
        FraudAlert alert = findAlert(alertId);
        Map<String, Object> oldValue = Map.of("status", alert.getStatus(),
                "assignedTo", nullableId(alert.getAssignedTo()), "dueAt", alert.getDueAt());
        User analyst = userRepository.findById(request.assignedTo())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.assignedTo()));
        if (analyst.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Fraud alerts can only be assigned to active users");
        }
        boolean eligible = userRoleRepository.existsByUserIdAndRoleName(analyst.getId(), RoleName.ADMIN)
                || userRoleRepository.existsByUserIdAndRoleName(analyst.getId(), RoleName.FRAUD_ANALYST)
                || userRoleRepository.existsByUserIdAndRoleName(analyst.getId(), RoleName.RISK_MANAGER);
        if (!eligible) {
            throw new BadRequestException("Assigned user must have a fraud investigation role");
        }
        mutate(() -> alert.assignTo(analyst, request.dueAt()));
        FraudAlertResponse response = saveResponse(alert);
        auditService.log(AuditAction.ALERT_ASSIGNED, "FraudAlert", alert.getId(), oldValue,
                Map.of("status", alert.getStatus(), "assignedTo", analyst.getId(), "dueAt", alert.getDueAt()));
        return response;
    }

    @Transactional
    public FraudAlertResponse updateStatus(UUID alertId, UpdateFraudAlertStatusRequest request) {
        FraudAlert alert = findAlert(alertId);
        if (alert.getStatus() == request.status()) {
            return FraudAlertResponse.from(alert, Instant.now());
        }
        switch (request.status()) {
            case IN_REVIEW -> mutate(alert::startReview);
            case CONVERTED_TO_CASE ->
                    throw new BadRequestException("Create a linked investigation case to convert an alert");
            case ASSIGNED -> throw new BadRequestException("Use the assign endpoint to assign an alert");
            case ESCALATED -> throw new BadRequestException("Use the escalate endpoint to escalate an alert");
            case CLOSED_FRAUD, CLOSED_FALSE_POSITIVE ->
                    throw new BadRequestException("Use the close endpoint to close an alert");
            case NEW -> throw new BadRequestException("Alerts cannot transition back to NEW");
        }
        return saveResponse(alert);
    }

    @Transactional
    public FraudAlertResponse close(UUID alertId, CloseFraudAlertRequest request) {
        FraudAlert alert = findAlert(alertId);
        FraudAlertStatus oldStatus = alert.getStatus();
        mutate(() -> alert.close(request.status(), request.resolution()));
        FraudAlertResponse response = saveResponse(alert);
        auditService.log(AuditAction.ALERT_CLOSED, "FraudAlert", alert.getId(),
                Map.of("status", oldStatus), Map.of("status", alert.getStatus(), "resolution", request.resolution()));
        return response;
    }

    @Transactional
    public FraudAlertResponse escalate(UUID alertId, EscalateFraudAlertRequest request) {
        FraudAlert alert = findAlert(alertId);
        mutate(() -> alert.escalate(request.reason(), request.dueAt()));
        return saveResponse(alert);
    }

    private FraudAlert findAlert(UUID alertId) {
        return alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Fraud alert", alertId));
    }

    private FraudAlertResponse saveResponse(FraudAlert alert) {
        return FraudAlertResponse.from(alertRepository.saveAndFlush(alert), Instant.now());
    }

    private void mutate(Runnable operation) {
        try {
            operation.run();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new BadRequestException(exception.getMessage(), exception);
        }
    }

    private String nextAlertNumber() {
        return "FRA-" + ALERT_DATE.format(Instant.now()) + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private Object nullableId(User user) {
        return user == null ? "unassigned" : user.getId();
    }

    private FraudAlertSeverity severity(RiskBand riskBand) {
        return switch (riskBand) {
            case LOW -> FraudAlertSeverity.LOW;
            case MEDIUM -> FraudAlertSeverity.MEDIUM;
            case HIGH -> FraudAlertSeverity.HIGH;
            case CRITICAL -> FraudAlertSeverity.CRITICAL;
        };
    }

    private Duration sla(RiskBand riskBand) {
        return switch (riskBand) {
            case LOW -> Duration.ofHours(72);
            case MEDIUM -> Duration.ofHours(24);
            case HIGH -> Duration.ofHours(8);
            case CRITICAL -> Duration.ofHours(2);
        };
    }

    private String initialSummary(TransactionRiskScore riskScore) {
        String summary = "Automated fraud alert for transaction "
                + riskScore.getTransaction().getTransactionReference() + ". "
                + riskScore.getExplanationSummary();
        return summary.length() <= 2000 ? summary : summary.substring(0, 2000);
    }
}
