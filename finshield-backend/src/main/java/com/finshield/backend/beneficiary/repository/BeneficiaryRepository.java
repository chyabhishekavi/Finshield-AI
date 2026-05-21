package com.finshield.backend.beneficiary.repository;

import com.finshield.backend.beneficiary.domain.Beneficiary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, UUID> {

    Page<Beneficiary> findAllByCustomerId(UUID customerId, Pageable pageable);

    Optional<Beneficiary> findByIdAndCustomerId(UUID id, UUID customerId);

    Optional<Beneficiary> findByCustomerIdAndBeneficiaryAccountNumber(
            UUID customerId,
            String beneficiaryAccountNumber
    );

    boolean existsByCustomerIdAndBeneficiaryAccountNumber(UUID customerId, String beneficiaryAccountNumber);
}
