package com.finshield.backend.beneficiary;

import com.finshield.backend.beneficiary.api.AddBeneficiaryRequest;
import com.finshield.backend.beneficiary.api.BeneficiaryResponse;
import com.finshield.backend.beneficiary.api.UpdateBeneficiaryRequest;
import com.finshield.backend.beneficiary.domain.Beneficiary;
import com.finshield.backend.beneficiary.domain.BeneficiaryStatus;
import com.finshield.backend.beneficiary.repository.BeneficiaryRepository;
import com.finshield.backend.common.api.PageResponse;
import com.finshield.backend.common.exception.BadRequestException;
import com.finshield.backend.common.exception.ResourceNotFoundException;
import com.finshield.backend.customer.domain.Customer;
import com.finshield.backend.customer.repository.CustomerRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final CustomerRepository customerRepository;

    public BeneficiaryService(
            BeneficiaryRepository beneficiaryRepository,
            CustomerRepository customerRepository
    ) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public BeneficiaryResponse add(UUID customerId, AddBeneficiaryRequest request) {
        Customer customer = findCustomer(customerId);
        String accountNumber = request.beneficiaryAccountNumber().trim().toUpperCase(Locale.ROOT);
        if (beneficiaryRepository.existsByCustomerIdAndBeneficiaryAccountNumber(customerId, accountNumber)) {
            throw new BadRequestException("This beneficiary account is already registered for the customer");
        }

        Beneficiary beneficiary = new Beneficiary(
                customer,
                request.beneficiaryName(),
                accountNumber,
                request.bankName(),
                request.ifscCode(),
                Instant.now(),
                request.riskScore() == null ? BigDecimal.ZERO : request.riskScore(),
                request.status() == null ? BeneficiaryStatus.PENDING_VERIFICATION : request.status()
        );

        try {
            return BeneficiaryResponse.from(beneficiaryRepository.saveAndFlush(beneficiary));
        } catch (DataIntegrityViolationException exception) {
            throw new BadRequestException(
                    "This beneficiary account is already registered for the customer", exception);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<BeneficiaryResponse> list(UUID customerId, int page, int size) {
        findCustomer(customerId);
        return PageResponse.from(
                beneficiaryRepository.findAllByCustomerId(
                        customerId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "addedAt"))
                ),
                BeneficiaryResponse::from
        );
    }

    @Transactional
    public BeneficiaryResponse update(
            UUID customerId,
            UUID beneficiaryId,
            UpdateBeneficiaryRequest request
    ) {
        Beneficiary beneficiary = beneficiaryRepository.findByIdAndCustomerId(beneficiaryId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary", beneficiaryId));

        if (allFieldsNull(request)) {
            throw new BadRequestException("At least one beneficiary field must be provided");
        }

        beneficiary.updateProfile(
                request.beneficiaryName() == null ? beneficiary.getBeneficiaryName() : request.beneficiaryName(),
                request.bankName() == null ? beneficiary.getBankName() : request.bankName(),
                request.ifscCode() == null ? beneficiary.getIfscCode() : request.ifscCode()
        );
        beneficiary.updateRisk(
                request.riskScore() == null ? beneficiary.getRiskScore() : request.riskScore(),
                request.status() == null ? beneficiary.getStatus() : request.status()
        );
        return BeneficiaryResponse.from(beneficiaryRepository.saveAndFlush(beneficiary));
    }

    private Customer findCustomer(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
    }

    private boolean allFieldsNull(UpdateBeneficiaryRequest request) {
        return request.beneficiaryName() == null
                && request.bankName() == null
                && request.ifscCode() == null
                && request.riskScore() == null
                && request.status() == null;
    }
}
