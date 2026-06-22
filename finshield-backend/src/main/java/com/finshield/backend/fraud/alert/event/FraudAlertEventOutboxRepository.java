package com.finshield.backend.fraud.alert.event;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.List;
import java.util.UUID;

public interface FraudAlertEventOutboxRepository extends JpaRepository<FraudAlertEventOutbox, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<FraudAlertEventOutbox> findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
}
