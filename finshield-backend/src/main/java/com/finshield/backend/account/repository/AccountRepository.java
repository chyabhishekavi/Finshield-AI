package com.finshield.backend.account.repository;

import com.finshield.backend.account.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findAllByCustomerIdOrderByOpenedAtDesc(UUID customerId);

    boolean existsByAccountNumber(String accountNumber);

    Optional<Account> findByAccountNumber(String accountNumber);
}
