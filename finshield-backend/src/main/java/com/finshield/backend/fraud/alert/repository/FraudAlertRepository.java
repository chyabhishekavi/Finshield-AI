package com.finshield.backend.fraud.alert.repository;

import com.finshield.backend.fraud.alert.domain.FraudAlert;
import com.finshield.backend.fraud.alert.domain.FraudAlertSeverity;
import com.finshield.backend.fraud.alert.domain.FraudAlertStatus;
import com.finshield.backend.transaction.domain.RiskBand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID> {

    Optional<FraudAlert> findByTransactionId(UUID transactionId);

    long countByStatusIn(Set<FraudAlertStatus> statuses);

    long countBySeverity(FraudAlertSeverity severity);

    @Query("select a.status as status, count(a) as alertCount from FraudAlert a group by a.status")
    java.util.List<AlertStatusAggregate> countByStatusGrouped();

    interface AlertStatusAggregate {
        FraudAlertStatus getStatus();
        long getAlertCount();
    }

    @Query("""
            select a from FraudAlert a
            where (:query = ''
                    or lower(a.alertNumber) like lower(concat('%', :query, '%'))
                    or lower(a.transaction.transactionReference) like lower(concat('%', :query, '%')))
              and (:status is null or a.status = :status)
              and (:severity is null or a.severity = :severity)
              and (:riskBand is null or a.riskBand = :riskBand)
              and (:assignedToId is null or a.assignedTo.id = :assignedToId)
              and (:customerId is null or a.customer.id = :customerId)
              and (:overdue is null
                    or (:overdue = true and a.dueAt < :now and a.status not in :terminalStatuses)
                    or (:overdue = false and (a.dueAt >= :now or a.status in :terminalStatuses)))
            """)
    Page<FraudAlert> search(
            @Param("query") String query,
            @Param("status") FraudAlertStatus status,
            @Param("severity") FraudAlertSeverity severity,
            @Param("riskBand") RiskBand riskBand,
            @Param("assignedToId") UUID assignedToId,
            @Param("customerId") UUID customerId,
            @Param("overdue") Boolean overdue,
            @Param("now") Instant now,
            @Param("terminalStatuses") Set<FraudAlertStatus> terminalStatuses,
            Pageable pageable
    );
}
