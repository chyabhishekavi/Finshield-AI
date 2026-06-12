package com.finshield.backend.fraud.repository;

import com.finshield.backend.fraud.domain.FraudRule;
import com.finshield.backend.fraud.domain.FraudRuleType;
import com.finshield.backend.fraud.domain.RuleSeverity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface FraudRuleRepository extends JpaRepository<FraudRule, UUID> {

    Optional<FraudRule> findByRuleCode(String ruleCode);

    boolean existsByRuleCode(String ruleCode);

    List<FraudRule> findAllByActiveTrueOrderByRuleCodeAsc();

    @Query("""
            select r from FraudRule r
            where (:query is null
                or lower(r.ruleCode) like lower(concat('%', :query, '%'))
                or lower(r.ruleName) like lower(concat('%', :query, '%'))
                or lower(r.description) like lower(concat('%', :query, '%')))
              and (:active is null or r.active = :active)
              and (:ruleType is null or r.ruleType = :ruleType)
              and (:severity is null or r.severity = :severity)
            """)
    Page<FraudRule> search(
            @Param("query") String query,
            @Param("active") Boolean active,
            @Param("ruleType") FraudRuleType ruleType,
            @Param("severity") RuleSeverity severity,
            Pageable pageable
    );
}
