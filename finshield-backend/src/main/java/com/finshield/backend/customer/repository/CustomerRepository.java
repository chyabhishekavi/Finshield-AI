package com.finshield.backend.customer.repository;

import com.finshield.backend.customer.domain.Customer;
import com.finshield.backend.customer.domain.CustomerRiskLevel;
import com.finshield.backend.customer.domain.KycStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    boolean existsByCustomerNumber(String customerNumber);

    Optional<Customer> findByCustomerNumber(String customerNumber);

    @Query("""
            select c from Customer c
            where (:query = ''
                or lower(c.customerNumber) like lower(concat('%', :query, '%'))
                or lower(c.fullName) like lower(concat('%', :query, '%'))
                or lower(c.email) like lower(concat('%', :query, '%'))
                or c.phone like concat('%', :query, '%'))
              and (:kycStatus is null or c.kycStatus = :kycStatus)
              and (:riskLevel is null or c.customerRiskLevel = :riskLevel)
            """)
    Page<Customer> search(
            @Param("query") String query,
            @Param("kycStatus") KycStatus kycStatus,
            @Param("riskLevel") CustomerRiskLevel riskLevel,
            Pageable pageable
    );
}
