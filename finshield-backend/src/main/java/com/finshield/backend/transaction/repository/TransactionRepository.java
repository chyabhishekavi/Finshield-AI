package com.finshield.backend.transaction.repository;

import com.finshield.backend.transaction.domain.RiskBand;
import com.finshield.backend.transaction.domain.Transaction;
import com.finshield.backend.transaction.domain.TransactionStatus;
import com.finshield.backend.transaction.domain.TransactionType;
import com.finshield.backend.transaction.domain.TransactionChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByTransactionReference(String transactionReference);

    boolean existsByTransactionReference(String transactionReference);

    long countByRiskBandIn(Set<RiskBand> riskBands);

    List<Transaction> findAllByRiskBandInOrderByInitiatedAtDesc(
            Set<RiskBand> riskBands,
            Pageable pageable
    );

    long countBySourceAccountIdAndInitiatedAtBetween(
            UUID sourceAccountId,
            Instant fromTime,
            Instant toTime
    );

    @Query("""
            select avg(t.amount) from Transaction t
            where t.sourceAccount.customer.id = :customerId
              and t.id <> :transactionId
              and t.initiatedAt < :beforeTime
            """)
    Double findAverageAmountByCustomerIdExcludingTransaction(
            @Param("customerId") UUID customerId,
            @Param("transactionId") UUID transactionId,
            @Param("beforeTime") Instant beforeTime
    );

    @Query("""
            select t from Transaction t
            where (t.sourceAccount.id = :accountId
                    or t.destinationAccountNumber = :accountNumber)
              and t.initiatedAt between :fromTime and :toTime
              and t.status not in :excludedStatuses
            order by t.initiatedAt asc
            """)
    List<Transaction> findAccountActivity(
            @Param("accountId") UUID accountId,
            @Param("accountNumber") String accountNumber,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime,
            @Param("excludedStatuses") Set<TransactionStatus> excludedStatuses
    );

    @Query("""
            select t from Transaction t
            where (:sourceAccountId is null or t.sourceAccount.id = :sourceAccountId)
              and (:query = ''
                    or lower(t.transactionReference) like lower(concat('%', :query, '%'))
                    or lower(t.sourceAccount.customer.fullName) like lower(concat('%', :query, '%'))
                    or lower(t.sourceAccount.customer.customerNumber) like lower(concat('%', :query, '%')))
              and (:status is null or t.status = :status)
              and (:type is null or t.transactionType = :type)
              and (:riskBand is null or t.riskBand = :riskBand)
              and (:channel is null or t.channel = :channel)
              and (:minAmount is null or t.amount >= :minAmount)
              and (:maxAmount is null or t.amount <= :maxAmount)
              and t.initiatedAt >= coalesce(:fromTime, t.initiatedAt)
              and t.initiatedAt <= coalesce(:toTime, t.initiatedAt)
            """)
    Page<Transaction> search(
            @Param("sourceAccountId") UUID sourceAccountId,
            @Param("query") String query,
            @Param("status") TransactionStatus status,
            @Param("type") TransactionType type,
            @Param("riskBand") RiskBand riskBand,
            @Param("channel") TransactionChannel channel,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime,
            Pageable pageable
    );
}
