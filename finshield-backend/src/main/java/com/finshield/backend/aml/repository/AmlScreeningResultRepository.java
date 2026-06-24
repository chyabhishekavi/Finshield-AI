package com.finshield.backend.aml.repository;

import com.finshield.backend.aml.domain.AmlScreeningResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AmlScreeningResultRepository extends JpaRepository<AmlScreeningResult, UUID> {

    List<AmlScreeningResult> findAllByScreeningReferenceOrderByMatchScoreDesc(UUID screeningReference);

    List<AmlScreeningResult> findTop20ByCustomerIdOrderByScreenedAtDesc(UUID customerId);
}
