package com.finshield.backend.risk.repository;

import com.finshield.backend.risk.domain.TransactionRiskScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

public interface TransactionRiskScoreRepository extends JpaRepository<TransactionRiskScore, UUID> {

    Optional<TransactionRiskScore> findBySourceEventId(UUID sourceEventId);

    List<TransactionRiskScore> findAllByTransactionIdOrderByScoredAtDesc(UUID transactionId);

    Optional<TransactionRiskScore> findFirstByTransactionIdOrderByScoredAtDesc(UUID transactionId);

    List<TransactionRiskScore> findAllByScoredAtBetweenOrderByScoredAtAsc(Instant from, Instant to);
}
