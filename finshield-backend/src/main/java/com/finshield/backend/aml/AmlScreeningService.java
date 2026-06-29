package com.finshield.backend.aml;

import com.finshield.backend.aml.api.AmlScreeningResponse;
import com.finshield.backend.aml.domain.AmlMatchType;
import com.finshield.backend.aml.domain.AmlScreeningResult;
import com.finshield.backend.aml.domain.AmlScreeningStatus;
import com.finshield.backend.aml.domain.AmlSubjectType;
import com.finshield.backend.aml.domain.AmlWatchlistEntry;
import com.finshield.backend.aml.matching.AmlNameMatchingService;
import com.finshield.backend.aml.matching.NameMatch;
import com.finshield.backend.aml.repository.AmlScreeningResultRepository;
import com.finshield.backend.aml.repository.AmlWatchlistEntryRepository;
import com.finshield.backend.beneficiary.domain.Beneficiary;
import com.finshield.backend.beneficiary.repository.BeneficiaryRepository;
import com.finshield.backend.common.exception.ResourceNotFoundException;
import com.finshield.backend.customer.domain.Customer;
import com.finshield.backend.customer.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class AmlScreeningService {

    private final AmlWatchlistEntryRepository watchlistRepository;
    private final AmlScreeningResultRepository screeningRepository;
    private final CustomerRepository customerRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final AmlNameMatchingService matchingService;

    public AmlScreeningService(
            AmlWatchlistEntryRepository watchlistRepository,
            AmlScreeningResultRepository screeningRepository,
            CustomerRepository customerRepository,
            BeneficiaryRepository beneficiaryRepository,
            AmlNameMatchingService matchingService
    ) {
        this.watchlistRepository = watchlistRepository;
        this.screeningRepository = screeningRepository;
        this.customerRepository = customerRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.matchingService = matchingService;
    }

    @Transactional
    public AmlScreeningResponse screenCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        return screen(AmlSubjectType.CUSTOMER, customerId, customer.getFullName(), customer, null);
    }

    @Transactional
    public AmlScreeningResponse screenBeneficiary(UUID beneficiaryId) {
        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary", beneficiaryId));
        return screen(AmlSubjectType.BENEFICIARY, beneficiaryId,
                beneficiary.getBeneficiaryName(), null, beneficiary);
    }

    private AmlScreeningResponse screen(
            AmlSubjectType subjectType,
            UUID subjectId,
            String subjectName,
            Customer customer,
            Beneficiary beneficiary
    ) {
        UUID screeningReference = UUID.randomUUID();
        Instant screenedAt = Instant.now();
        List<AmlScreeningResult> results = new ArrayList<>();

        for (AmlWatchlistEntry entry : watchlistRepository.findAllByActiveTrueOrderByNameAsc()) {
            NameMatch match = matchingService.match(subjectName, entry.getName());
            if (match.matched()) {
                results.add(new AmlScreeningResult(
                        screeningReference,
                        subjectType,
                        customer,
                        beneficiary,
                        subjectName,
                        entry,
                        match.matchType(),
                        match.score(),
                        AmlScreeningStatus.PENDING_REVIEW,
                        matchReason(match, entry),
                        screenedAt
                ));
            }
        }

        if (results.isEmpty()) {
            results.add(new AmlScreeningResult(
                    screeningReference,
                    subjectType,
                    customer,
                    beneficiary,
                    subjectName,
                    null,
                    AmlMatchType.NONE,
                    new BigDecimal("0.00"),
                    AmlScreeningStatus.NO_MATCH,
                    "No active watchlist entries matched the screened name",
                    screenedAt
            ));
        }

        List<AmlScreeningResult> saved = screeningRepository.saveAllAndFlush(results).stream()
                .sorted(Comparator.comparing(AmlScreeningResult::getMatchScore).reversed())
                .toList();
        return new AmlScreeningResponse(
                screeningReference,
                subjectType,
                subjectId,
                subjectName,
                saved.stream().anyMatch(result -> result.getMatchType() != AmlMatchType.NONE),
                saved.stream().map(AmlScreeningResponse.ScreeningMatchResponse::from).toList(),
                screenedAt
        );
    }

    private String matchReason(NameMatch match, AmlWatchlistEntry entry) {
        return "%s name match against %s entry %s (score %s)".formatted(
                match.matchType() == AmlMatchType.EXACT ? "Exact" : "Fuzzy",
                entry.getListType(),
                entry.getIdentifier(),
                match.score()
        );
    }
}
