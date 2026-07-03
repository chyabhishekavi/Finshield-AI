package com.finshield.backend.casework.repository;

import com.finshield.backend.casework.domain.CasePriority;
import com.finshield.backend.casework.domain.InvestigationCase;
import com.finshield.backend.casework.domain.InvestigationCaseStatus;
import com.finshield.backend.casework.domain.InvestigationCaseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.time.Instant;

public interface InvestigationCaseRepository extends JpaRepository<InvestigationCase, UUID> {
    Optional<InvestigationCase> findByLinkedAlertId(UUID alertId);

    long countByStatusNot(InvestigationCaseStatus status);

    List<InvestigationCase> findAllByCreatedAtBetweenOrderByCreatedAtAsc(Instant from, Instant to);

    @Query("""
            select c from InvestigationCase c
            where c.status <> :closedStatus
               or c.createdAt between :fromTime and :toTime
            order by c.createdAt asc
            """)
    List<InvestigationCase> findForSlaSummary(
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime,
            @Param("closedStatus") InvestigationCaseStatus closedStatus
    );

    @Query("""
            select c from InvestigationCase c
            where (:query = '' or lower(c.caseNumber) like lower(concat('%', :query, '%')))
              and (:type is null or c.caseType = :type)
              and (:status is null or c.status = :status)
              and (:priority is null or c.priority = :priority)
              and (:customerId is null or c.customer.id = :customerId)
              and (:assignedToId is null or c.assignedTo.id = :assignedToId)
            """)
    Page<InvestigationCase> search(@Param("query") String query,
            @Param("type") InvestigationCaseType type,
            @Param("status") InvestigationCaseStatus status,
            @Param("priority") CasePriority priority,
            @Param("customerId") UUID customerId,
            @Param("assignedToId") UUID assignedToId,
            Pageable pageable);
}
