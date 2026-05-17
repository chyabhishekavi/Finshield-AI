package com.finshield.backend.customer;

import com.finshield.backend.account.api.AccountSummaryResponse;
import com.finshield.backend.account.domain.Account;
import com.finshield.backend.account.repository.AccountRepository;
import com.finshield.backend.common.api.PageResponse;
import com.finshield.backend.common.exception.BadRequestException;
import com.finshield.backend.common.exception.ResourceNotFoundException;
import com.finshield.backend.customer.api.CreateCustomerRequest;
import com.finshield.backend.customer.api.Customer360Response;
import com.finshield.backend.customer.api.CustomerResponse;
import com.finshield.backend.customer.api.CustomerRiskHistoryResponse;
import com.finshield.backend.customer.api.CustomerSummaryResponse;
import com.finshield.backend.customer.api.UpdateCustomerRiskRequest;
import com.finshield.backend.customer.domain.Customer;
import com.finshield.backend.customer.domain.CustomerRiskHistory;
import com.finshield.backend.customer.domain.CustomerRiskLevel;
import com.finshield.backend.customer.domain.KycStatus;
import com.finshield.backend.customer.repository.CustomerRepository;
import com.finshield.backend.customer.repository.CustomerRiskHistoryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final CustomerRiskHistoryRepository riskHistoryRepository;

    public CustomerService(
            CustomerRepository customerRepository,
            AccountRepository accountRepository,
            CustomerRiskHistoryRepository riskHistoryRepository
    ) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.riskHistoryRepository = riskHistoryRepository;
    }

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        Customer customer = new Customer(
                request.customerNumber(),
                request.fullName(),
                request.dateOfBirth(),
                request.email(),
                request.phone(),
                request.kycStatus(),
                request.customerRiskLevel(),
                request.occupation(),
                request.annualIncomeRange(),
                request.country(),
                request.city(),
                request.branchCode()
        );

        if (customerRepository.existsByCustomerNumber(customer.getCustomerNumber())) {
            throw new BadRequestException("A customer with this customer number already exists");
        }

        try {
            return CustomerResponse.from(customerRepository.saveAndFlush(customer));
        } catch (DataIntegrityViolationException exception) {
            throw new BadRequestException("A customer with this customer number already exists", exception);
        }
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(UUID customerId) {
        return CustomerResponse.from(findCustomer(customerId));
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerSummaryResponse> search(
            String query,
            KycStatus kycStatus,
            CustomerRiskLevel riskLevel,
            int page,
            int size
    ) {
        String normalizedQuery = query == null || query.isBlank() ? "" : query.trim();
        Page<Customer> result = customerRepository.search(
                normalizedQuery,
                kycStatus,
                riskLevel,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResponse.from(result, CustomerSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public Customer360Response get360Profile(UUID customerId) {
        Customer customer = findCustomer(customerId);
        List<Account> accounts = accountRepository.findAllByCustomerIdOrderByOpenedAtDesc(customerId);
        List<AccountSummaryResponse> accountResponses = accounts.stream()
                .map(AccountSummaryResponse::from)
                .toList();

        Map<String, BigDecimal> balancesByCurrency = new TreeMap<>();
        accounts.forEach(account -> balancesByCurrency.merge(
                account.getCurrency(), account.getBalance(), BigDecimal::add));

        List<CustomerRiskHistoryResponse> riskHistory =
                riskHistoryRepository.findTop20ByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                        .map(CustomerRiskHistoryResponse::from)
                        .toList();

        return new Customer360Response(
                CustomerResponse.from(customer),
                accountResponses,
                accountResponses.size(),
                balancesByCurrency,
                riskHistory
        );
    }

    @Transactional
    public CustomerResponse updateRiskLevel(UUID customerId, UpdateCustomerRiskRequest request) {
        Customer customer = findCustomer(customerId);
        CustomerRiskLevel previousLevel = customer.getCustomerRiskLevel();
        if (previousLevel == request.riskLevel()) {
            throw new BadRequestException("Customer already has the requested risk level");
        }

        customer.changeRiskLevel(request.riskLevel());
        customerRepository.saveAndFlush(customer);
        riskHistoryRepository.saveAndFlush(new CustomerRiskHistory(
                customer,
                previousLevel,
                request.riskLevel(),
                request.reason()
        ));
        return CustomerResponse.from(customer);
    }

    private Customer findCustomer(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
    }
}
