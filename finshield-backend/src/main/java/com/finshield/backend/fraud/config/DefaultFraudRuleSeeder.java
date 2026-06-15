package com.finshield.backend.fraud.config;

import com.finshield.backend.fraud.domain.FraudRule;
import com.finshield.backend.fraud.domain.FraudRuleType;
import com.finshield.backend.fraud.domain.RuleOperator;
import com.finshield.backend.fraud.domain.RuleSeverity;
import com.finshield.backend.fraud.repository.FraudRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DefaultFraudRuleSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultFraudRuleSeeder.class);

    private final FraudRuleRepository ruleRepository;

    public DefaultFraudRuleSeeder(FraudRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<FraudRule> missingRules = defaults().stream()
                .filter(rule -> !ruleRepository.existsByRuleCode(rule.getRuleCode()))
                .toList();
        if (!missingRules.isEmpty()) {
            ruleRepository.saveAll(missingRules);
            log.info("Seeded {} default fraud rules", missingRules.size());
        }
    }

    private List<FraudRule> defaults() {
        return List.of(
                rule(
                        "HIGH_AMOUNT", "High amount transaction",
                        "Flags transactions whose normalized amount exceeds the configured high-value threshold",
                        FraudRuleType.THRESHOLD, "amount", RuleOperator.GREATER_THAN_OR_EQUAL,
                        "10000", "30.00", RuleSeverity.HIGH
                ),
                rule(
                        "NEW_DEVICE_HIGH_VALUE", "New device with high value",
                        "Detects high-value transactions initiated from a device not previously trusted",
                        FraudRuleType.COMPOSITE, "composite", RuleOperator.ALL_MATCH,
                        "{\"conditions\":["
                                + "{\"field\":\"device.new\",\"operator\":\"EQUALS\",\"value\":true},"
                                + "{\"field\":\"amount\",\"operator\":\"GREATER_THAN_OR_EQUAL\",\"value\":5000}"
                                + "]}",
                        "40.00", RuleSeverity.CRITICAL
                ),
                rule(
                        "VELOCITY_5_MIN", "Five-minute transaction velocity",
                        "Detects repeated transactions from the same source account within five minutes",
                        FraudRuleType.VELOCITY, "transaction.count.5m", RuleOperator.GREATER_THAN_OR_EQUAL,
                        "5", "25.00", RuleSeverity.HIGH
                ),
                rule(
                        "FIRST_TIME_BENEFICIARY", "First-time beneficiary",
                        "Flags transfers to a beneficiary with no prior relationship to the customer",
                        FraudRuleType.BEHAVIORAL, "beneficiary.firstTime", RuleOperator.EQUALS,
                        "true", "20.00", RuleSeverity.MEDIUM
                ),
                rule(
                        "GEO_LOCATION_MISMATCH", "Geographic location mismatch",
                        "Detects transaction geography inconsistent with the customer's established profile",
                        FraudRuleType.GEOLOCATION, "geoLocation.matchesProfile", RuleOperator.EQUALS,
                        "false", "35.00", RuleSeverity.HIGH
                )
        );
    }

    private FraudRule rule(
            String code,
            String name,
            String description,
            FraudRuleType type,
            String conditionField,
            RuleOperator operator,
            String threshold,
            String scoreImpact,
            RuleSeverity severity
    ) {
        return new FraudRule(
                code,
                name,
                description,
                type,
                conditionField,
                operator,
                threshold,
                new BigDecimal(scoreImpact),
                true,
                severity
        );
    }
}
