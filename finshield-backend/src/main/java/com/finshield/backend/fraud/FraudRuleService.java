package com.finshield.backend.fraud;

import com.finshield.backend.common.api.PageResponse;
import com.finshield.backend.audit.AuditService;
import com.finshield.backend.audit.domain.AuditAction;
import com.finshield.backend.common.exception.BadRequestException;
import com.finshield.backend.common.exception.ResourceNotFoundException;
import com.finshield.backend.fraud.api.CreateFraudRuleRequest;
import com.finshield.backend.fraud.api.FraudRuleResponse;
import com.finshield.backend.fraud.api.UpdateFraudRuleRequest;
import com.finshield.backend.fraud.domain.FraudRule;
import com.finshield.backend.fraud.domain.FraudRuleType;
import com.finshield.backend.fraud.domain.RuleSeverity;
import com.finshield.backend.fraud.repository.FraudRuleRepository;
import com.finshield.backend.notification.NotificationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class FraudRuleService {

    private final FraudRuleRepository ruleRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public FraudRuleService(FraudRuleRepository ruleRepository, AuditService auditService,
            NotificationService notificationService) {
        this.ruleRepository = ruleRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional
    public FraudRuleResponse create(CreateFraudRuleRequest request) {
        String ruleCode = request.ruleCode().trim().toUpperCase(Locale.ROOT);
        if (ruleRepository.existsByRuleCode(ruleCode)) {
            throw new BadRequestException("A fraud rule with this code already exists");
        }

        FraudRule rule = new FraudRule(
                ruleCode,
                request.ruleName(),
                request.description(),
                request.ruleType(),
                request.conditionField(),
                request.operator(),
                request.thresholdValue(),
                request.scoreImpact(),
                request.active(),
                request.severity()
        );

        try {
            FraudRule saved = ruleRepository.saveAndFlush(rule);
            auditService.log(AuditAction.FRAUD_RULE_CREATED, "FraudRule", saved.getId(), null, snapshot(saved));
            return FraudRuleResponse.from(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new BadRequestException("A fraud rule with this code already exists", exception);
        }
    }

    @Transactional(readOnly = true)
    public FraudRuleResponse getById(UUID ruleId) {
        return FraudRuleResponse.from(findRule(ruleId));
    }

    @Transactional(readOnly = true)
    public PageResponse<FraudRuleResponse> search(
            String query,
            Boolean active,
            FraudRuleType ruleType,
            RuleSeverity severity,
            int page,
            int size
    ) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        return PageResponse.from(
                ruleRepository.search(
                        normalizedQuery,
                        active,
                        ruleType,
                        severity,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "ruleCode"))
                ),
                FraudRuleResponse::from
        );
    }

    @Transactional
    public FraudRuleResponse update(UUID ruleId, UpdateFraudRuleRequest request) {
        FraudRule rule = findRule(ruleId);
        Map<String, Object> oldValue = snapshot(rule);
        rule.update(
                request.ruleName(),
                request.description(),
                request.ruleType(),
                request.conditionField(),
                request.operator(),
                request.thresholdValue(),
                request.scoreImpact(),
                request.active(),
                request.severity()
        );
        FraudRule saved = ruleRepository.saveAndFlush(rule);
        auditService.log(AuditAction.FRAUD_RULE_UPDATED, "FraudRule", saved.getId(), oldValue, snapshot(saved));
        notificationService.notifyRuleUpdated(saved);
        return FraudRuleResponse.from(saved);
    }

    @Transactional
    public void deactivate(UUID ruleId) {
        FraudRule rule = findRule(ruleId);
        Map<String, Object> oldValue = snapshot(rule);
        rule.deactivate();
        ruleRepository.saveAndFlush(rule);
        auditService.log(AuditAction.FRAUD_RULE_UPDATED, "FraudRule", rule.getId(), oldValue, snapshot(rule));
        notificationService.notifyRuleUpdated(rule);
    }

    private FraudRule findRule(UUID ruleId) {
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Fraud rule", ruleId));
    }

    private Map<String, Object> snapshot(FraudRule rule) {
        return Map.ofEntries(
                Map.entry("ruleCode", rule.getRuleCode()), Map.entry("ruleName", rule.getRuleName()),
                Map.entry("description", rule.getDescription()),
                Map.entry("ruleType", rule.getRuleType()), Map.entry("conditionField", rule.getConditionField()),
                Map.entry("operator", rule.getOperator()), Map.entry("thresholdValue", rule.getThresholdValue()),
                Map.entry("scoreImpact", rule.getScoreImpact()), Map.entry("active", rule.isActive()),
                Map.entry("severity", rule.getSeverity()));
    }
}
