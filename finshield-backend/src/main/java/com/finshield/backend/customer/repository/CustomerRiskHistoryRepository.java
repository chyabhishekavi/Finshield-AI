package com.finshield.backend.customer.repository;

import com.finshield.backend.customer.domain.CustomerRiskHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CustomerRiskHistoryRepository extends JpaRepository<CustomerRiskHistory, UUID> {

    List<CustomerRiskHistory> findTop20ByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
