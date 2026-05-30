package com.finshield.backend.transaction;

import com.finshield.backend.transaction.api.CreateTransactionRequest;
import com.finshield.backend.transaction.api.TransactionIngestRequest;
import com.finshield.backend.transaction.api.TransactionResponse;
import com.finshield.backend.transaction.domain.TransactionStatus;
import com.finshield.backend.transaction.event.TransactionEvent;
import com.finshield.backend.transaction.event.TransactionEventPublisher;
import com.finshield.backend.account.domain.Account;
import com.finshield.backend.account.repository.AccountRepository;
import com.finshield.backend.common.exception.BadRequestException;
import com.finshield.backend.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

@Service
public class TransactionIngestionService {

    private static final int EVENT_SCHEMA_VERSION = 1;

    private final TransactionService transactionService;
    private final TransactionEventPublisher eventPublisher;
    private final AccountRepository accountRepository;

    public TransactionIngestionService(
            TransactionService transactionService,
            TransactionEventPublisher eventPublisher,
            AccountRepository accountRepository
    ) {
        this.transactionService = transactionService;
        this.eventPublisher = eventPublisher;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public String ingest(TransactionIngestRequest request) {
        Instant initiatedAt = request.initiatedAt().truncatedTo(ChronoUnit.MILLIS);
        Account sourceAccount = resolveSourceAccount(request);
        String reference = request.transactionReference() == null || request.transactionReference().isBlank()
                ? "TXN-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8)
                : request.transactionReference();
        TransactionResponse transaction = transactionService.create(new CreateTransactionRequest(
                reference,
                sourceAccount.getId(),
                request.destinationAccountNumber(),
                request.beneficiaryName(),
                request.amount(),
                request.currency(),
                request.transactionType(),
                request.channel(),
                TransactionStatus.RECEIVED,
                request.deviceId(),
                request.ipAddress(),
                request.geoLocation(),
                initiatedAt
        ));

        eventPublisher.publish(new TransactionEvent(
                UUID.randomUUID(),
                EVENT_SCHEMA_VERSION,
                transaction.id(),
                transaction.transactionReference(),
                transaction.sourceAccountId(),
                request.destinationAccountNumber().trim().toUpperCase(Locale.ROOT),
                transaction.beneficiaryName(),
                transaction.amount(),
                transaction.currency(),
                transaction.transactionType(),
                transaction.channel(),
                TransactionStatus.RECEIVED,
                transaction.deviceId(),
                transaction.ipAddress(),
                transaction.geoLocation(),
                initiatedAt,
                Instant.now()
        ));

        return transaction.transactionReference();
    }

    private Account resolveSourceAccount(TransactionIngestRequest request) {
        Account account;
        if (request.sourceAccountId() != null) {
            account = accountRepository.findById(request.sourceAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Source account", request.sourceAccountId()));
        } else if (request.sourceAccountNumber() != null && !request.sourceAccountNumber().isBlank()) {
            account = accountRepository.findByAccountNumber(request.sourceAccountNumber().trim().toUpperCase(Locale.ROOT))
                    .orElseThrow(() -> new ResourceNotFoundException("Source account", request.sourceAccountNumber()));
        } else {
            throw new BadRequestException("sourceAccountId or sourceAccountNumber is required");
        }
        if (request.customerNumber() != null
                && !account.getCustomer().getCustomerNumber().equalsIgnoreCase(request.customerNumber().trim())) {
            throw new BadRequestException("Source account does not belong to the supplied customer");
        }
        return account;
    }
}
