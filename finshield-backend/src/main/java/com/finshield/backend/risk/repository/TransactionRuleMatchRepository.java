package com.finshield.backend.risk.repository;

import com.finshield.backend.risk.domain.TransactionRuleMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TransactionRuleMatchRepository extends JpaRepository<TransactionRuleMatch, UUID> {

    List<TransactionRuleMatch> findAllByTransactionRiskScoreIdOrderByScoreImpactDesc(
            UUID transactionRiskScoreId
    );

    List<TransactionRuleMatch> findAllByTransactionIdOrderByCreatedAtDesc(UUID transactionId);

    @Query("""
            select m.ruleCode as ruleCode, m.ruleName as ruleName,
                   count(m) as matchCount, sum(m.scoreImpact) as totalScoreImpact
            from TransactionRuleMatch m
            where m.createdAt >= :fromTime
            group by m.ruleCode, m.ruleName
            order by count(m) desc, sum(m.scoreImpact) desc
            """)
    List<TopRuleAggregate> findTopRules(@Param("fromTime") Instant fromTime, Pageable pageable);

    interface TopRuleAggregate {
        String getRuleCode();
        String getRuleName();
        long getMatchCount();
        BigDecimal getTotalScoreImpact();
    }
}
