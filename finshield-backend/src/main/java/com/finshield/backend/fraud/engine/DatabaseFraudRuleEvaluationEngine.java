package com.finshield.backend.fraud.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finshield.backend.account.domain.Account;
import com.finshield.backend.beneficiary.domain.Beneficiary;
import com.finshield.backend.customer.domain.Customer;
import com.finshield.backend.device.domain.CustomerDevice;
import com.finshield.backend.fraud.api.RuleEvaluationResult;
import com.finshield.backend.fraud.domain.FraudRule;
import com.finshield.backend.fraud.domain.RuleOperator;
import com.finshield.backend.fraud.repository.FraudRuleRepository;
import com.finshield.backend.risk.velocity.VelocityMetrics;
import com.finshield.backend.transaction.domain.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class DatabaseFraudRuleEvaluationEngine implements FraudRuleEvaluationEngine {

    private static final Logger log = LoggerFactory.getLogger(DatabaseFraudRuleEvaluationEngine.class);

    private final FraudRuleRepository ruleRepository;
    private final RuleFactExtractor factExtractor;
    private final ObjectMapper objectMapper;

    public DatabaseFraudRuleEvaluationEngine(
            FraudRuleRepository ruleRepository,
            RuleFactExtractor factExtractor,
            ObjectMapper objectMapper
    ) {
        this.ruleRepository = ruleRepository;
        this.factExtractor = factExtractor;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public RuleEvaluationResult evaluate(
            Transaction transaction,
            Customer customer,
            Account account,
            CustomerDevice device,
            Beneficiary beneficiary,
            VelocityMetrics velocityMetrics
    ) {
        Objects.requireNonNull(transaction, "transaction must not be null");
        Objects.requireNonNull(customer, "customer must not be null");
        Objects.requireNonNull(account, "account must not be null");
        Objects.requireNonNull(velocityMetrics, "velocityMetrics must not be null");

        Map<String, Object> facts = factExtractor.extract(
                transaction, customer, account, device, beneficiary, velocityMetrics);
        List<FraudRule> activeRules = ruleRepository.findAllByActiveTrueOrderByRuleCodeAsc();
        List<RuleEvaluationResult.MatchedRule> matches = new ArrayList<>();
        List<RuleEvaluationResult.RuleEvaluationError> errors = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;

        for (FraudRule rule : activeRules) {
            try {
                if (matches(rule, facts)) {
                    String reason = rule.getRuleName() + ": " + rule.getDescription();
                    matches.add(new RuleEvaluationResult.MatchedRule(
                            rule.getId(),
                            rule.getRuleCode(),
                            rule.getRuleName(),
                            rule.getRuleType(),
                            rule.getSeverity(),
                            rule.getScoreImpact(),
                            reason
                    ));
                    totalScore = totalScore.add(rule.getScoreImpact());
                }
            } catch (InvalidRuleConfigurationException exception) {
                log.warn("Fraud rule configuration is invalid. ruleCode={}, reason={}",
                        rule.getRuleCode(), exception.getMessage());
                errors.add(new RuleEvaluationResult.RuleEvaluationError(
                        rule.getRuleCode(), exception.getMessage()));
            }
        }

        List<String> reasons = matches.stream()
                .map(RuleEvaluationResult.MatchedRule::reason)
                .toList();
        return new RuleEvaluationResult(
                matches,
                reasons,
                totalScore.setScale(2),
                activeRules.size(),
                errors,
                Instant.now()
        );
    }

    private boolean matches(FraudRule rule, Map<String, Object> facts) {
        if (rule.getOperator() == RuleOperator.ALL_MATCH
                || rule.getOperator() == RuleOperator.ANY_MATCH) {
            return evaluateComposite(rule, facts);
        }
        return evaluateCondition(
                rule.getConditionField(),
                rule.getOperator(),
                rule.getThresholdValue(),
                facts
        );
    }

    private boolean evaluateComposite(FraudRule rule, Map<String, Object> facts) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rule.getThresholdValue());
        } catch (JsonProcessingException exception) {
            throw invalid("Composite threshold must be valid JSON");
        }

        JsonNode conditions = root.path("conditions");
        if (!conditions.isArray() || conditions.isEmpty()) {
            throw invalid("Composite threshold must contain a non-empty conditions array");
        }

        boolean allMatch = rule.getOperator() == RuleOperator.ALL_MATCH;
        for (JsonNode condition : conditions) {
            String field = requiredText(condition, "field");
            String operatorValue = requiredText(condition, "operator");
            String threshold = requiredValue(condition, "value");
            RuleOperator operator;
            try {
                operator = RuleOperator.valueOf(operatorValue.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw invalid("Unsupported composite operator: " + operatorValue);
            }
            if (operator == RuleOperator.ALL_MATCH || operator == RuleOperator.ANY_MATCH) {
                throw invalid("Nested composite operators are not supported");
            }

            boolean conditionMatched = evaluateCondition(field, operator, threshold, facts);
            if (allMatch && !conditionMatched) {
                return false;
            }
            if (!allMatch && conditionMatched) {
                return true;
            }
        }
        return allMatch;
    }

    private boolean evaluateCondition(
            String field,
            RuleOperator operator,
            String threshold,
            Map<String, Object> facts
    ) {
        if (!facts.containsKey(field)) {
            throw invalid("Unsupported condition field: " + field);
        }
        Object fact = facts.get(field);

        if (fact == null) {
            return switch (operator) {
                case EQUALS -> "null".equalsIgnoreCase(threshold);
                case NOT_EQUALS -> !"null".equalsIgnoreCase(threshold);
                default -> false;
            };
        }

        return switch (operator) {
            case EQUALS -> valuesEqual(fact, threshold);
            case NOT_EQUALS -> !valuesEqual(fact, threshold);
            case GREATER_THAN -> compare(fact, threshold) > 0;
            case GREATER_THAN_OR_EQUAL -> compare(fact, threshold) >= 0;
            case LESS_THAN -> compare(fact, threshold) < 0;
            case LESS_THAN_OR_EQUAL -> compare(fact, threshold) <= 0;
            case IN -> thresholdValues(threshold).stream().anyMatch(value -> valuesEqual(fact, value));
            case NOT_IN -> thresholdValues(threshold).stream().noneMatch(value -> valuesEqual(fact, value));
            case CONTAINS -> fact != null
                    && fact.toString().toLowerCase(Locale.ROOT)
                    .contains(threshold.toLowerCase(Locale.ROOT));
            case ALL_MATCH, ANY_MATCH -> throw invalid("Composite operator requires composite configuration");
        };
    }

    private boolean valuesEqual(Object fact, String threshold) {
        if (fact == null) {
            return "null".equalsIgnoreCase(threshold);
        }
        if (fact instanceof Number) {
            return decimal(fact.toString()).compareTo(decimal(threshold)) == 0;
        }
        if (fact instanceof Boolean booleanFact) {
            return booleanFact == bool(threshold);
        }
        if (fact instanceof Instant instantFact) {
            return instantFact.equals(instant(threshold));
        }
        return fact.toString().equalsIgnoreCase(threshold.trim());
    }

    private int compare(Object fact, String threshold) {
        if (fact == null) {
            return -1;
        }
        if (fact instanceof Number) {
            return decimal(fact.toString()).compareTo(decimal(threshold));
        }
        if (fact instanceof Instant instantFact) {
            return instantFact.compareTo(instant(threshold));
        }
        return fact.toString().compareToIgnoreCase(threshold.trim());
    }

    private List<String> thresholdValues(String threshold) {
        if (threshold.trim().startsWith("[")) {
            try {
                JsonNode values = objectMapper.readTree(threshold);
                if (!values.isArray()) {
                    throw invalid("IN threshold must be a JSON array or comma-separated values");
                }
                List<String> result = new ArrayList<>();
                values.forEach(value -> result.add(value.asText()));
                return result;
            } catch (JsonProcessingException exception) {
                throw invalid("IN threshold contains invalid JSON");
            }
        }
        return List.of(threshold.split(",")).stream().map(String::trim).toList();
    }

    private BigDecimal decimal(String value) {
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            throw invalid("Threshold must be numeric");
        }
    }

    private boolean bool(String value) {
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw invalid("Threshold must be true or false");
        }
        return Boolean.parseBoolean(value);
    }

    private Instant instant(String value) {
        try {
            return Instant.parse(value.trim());
        } catch (RuntimeException exception) {
            throw invalid("Threshold must be an ISO-8601 instant");
        }
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw invalid("Composite condition requires text field: " + field);
        }
        return value.asText();
    }

    private String requiredValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isContainerNode() || value.isNull()) {
            throw invalid("Composite condition requires scalar field: " + field);
        }
        return value.asText();
    }

    private InvalidRuleConfigurationException invalid(String message) {
        return new InvalidRuleConfigurationException(message);
    }

    private static final class InvalidRuleConfigurationException extends RuntimeException {
        private InvalidRuleConfigurationException(String message) {
            super(message);
        }
    }
}
