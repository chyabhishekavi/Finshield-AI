package com.finshield.backend.transaction;

import com.finshield.backend.account.domain.Account;
import com.finshield.backend.account.repository.AccountRepository;
import com.finshield.backend.audit.AuditService;
import com.finshield.backend.audit.domain.AuditAction;
import com.finshield.backend.common.api.PageResponse;
import com.finshield.backend.common.exception.BadRequestException;
import com.finshield.backend.common.exception.ResourceNotFoundException;
import com.finshield.backend.transaction.api.ApplyRiskDecisionRequest;
import com.finshield.backend.transaction.api.CreateTransactionRequest;
import com.finshield.backend.transaction.api.TransactionResponse;
import com.finshield.backend.transaction.api.TransactionRiskExplanationResponse;
import com.finshield.backend.transaction.api.UpdateTransactionStatusRequest;
import com.finshield.backend.transaction.domain.RiskBand;
import com.finshield.backend.transaction.domain.Transaction;
import com.finshield.backend.transaction.domain.TransactionStatus;
import com.finshield.backend.transaction.domain.TransactionType;
import com.finshield.backend.transaction.domain.TransactionChannel;
import com.finshield.backend.transaction.repository.TransactionRepository;
import com.finshield.backend.risk.repository.TransactionRiskScoreRepository;
import com.finshield.backend.risk.repository.TransactionRuleMatchRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AuditService auditService;
    private final TransactionRiskScoreRepository riskScoreRepository;
    private final TransactionRuleMatchRepository ruleMatchRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            AuditService auditService,
            TransactionRiskScoreRepository riskScoreRepository,
            TransactionRuleMatchRepository ruleMatchRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.auditService = auditService;
        this.riskScoreRepository = riskScoreRepository;
        this.ruleMatchRepository = ruleMatchRepository;
    }

    @Transactional
    public TransactionResponse create(CreateTransactionRequest request) {
        String reference = request.transactionReference().trim().toUpperCase(Locale.ROOT);
        if (transactionRepository.existsByTransactionReference(reference)) {
            throw new BadRequestException("A transaction with this reference already exists");
        }

        Account sourceAccount = accountRepository.findById(request.sourceAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Source account", request.sourceAccountId()));
        String destinationAccount = request.destinationAccountNumber().trim().toUpperCase(Locale.ROOT);
        if (sourceAccount.getAccountNumber().equalsIgnoreCase(destinationAccount)) {
            throw new BadRequestException("Source and destination accounts must be different");
        }

        Transaction transaction = new Transaction(
                reference,
                sourceAccount,
                destinationAccount,
                request.beneficiaryName(),
                request.amount(),
                request.currency(),
                request.transactionType(),
                request.channel(),
                request.status() == null ? TransactionStatus.RECEIVED : request.status(),
                request.deviceId(),
                request.ipAddress(),
                request.geoLocation(),
                request.initiatedAt()
        );

        try {
            return TransactionResponse.from(transactionRepository.saveAndFlush(transaction));
        } catch (DataIntegrityViolationException exception) {
            throw new BadRequestException("A transaction with this reference already exists", exception);
        }
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(UUID transactionId) {
        return TransactionResponse.from(findTransaction(transactionId));
    }

    @Transactional(readOnly = true)
    public TransactionResponse getByReference(String reference) {
        return transactionRepository.findByTransactionReference(reference.trim().toUpperCase(Locale.ROOT))
                .map(TransactionResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", reference));
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> search(
            UUID sourceAccountId,
            String query,
            TransactionStatus status,
            TransactionType type,
            RiskBand riskBand,
            TransactionChannel channel,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            Instant fromTime,
            Instant toTime,
            int page,
            int size
    ) {
        if (fromTime != null && toTime != null && fromTime.isAfter(toTime)) {
            throw new BadRequestException("fromTime must be before or equal to toTime");
        }
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new BadRequestException("minAmount must be less than or equal to maxAmount");
        }
        String normalizedQuery = query == null || query.isBlank() ? "" : query.trim();
        return PageResponse.from(
                transactionRepository.search(
                        sourceAccountId,
                        normalizedQuery,
                        status,
                        type,
                        riskBand,
                        channel,
                        minAmount,
                        maxAmount,
                        fromTime,
                        toTime,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "initiatedAt"))
                ),
                TransactionResponse::from
        );
    }

    @Transactional(readOnly = true)
    public TransactionRiskExplanationResponse riskExplanation(UUID transactionId) {
        findTransaction(transactionId);
        var score = riskScoreRepository.findFirstByTransactionIdOrderByScoredAtDesc(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction risk score", transactionId));
        return TransactionRiskExplanationResponse.from(score,
                ruleMatchRepository.findAllByTransactionRiskScoreIdOrderByScoreImpactDesc(score.getId()));
    }

    @Transactional
    public TransactionResponse applyRiskDecision(
            UUID transactionId,
            ApplyRiskDecisionRequest request
    ) {
        Transaction transaction = findTransaction(transactionId);
        Map<String, Object> oldValue = Map.of("riskScore", transaction.getRiskScore(),
                "riskBand", transaction.getRiskBand(), "decision", transaction.getDecision());
        transaction.applyRiskDecision(request.riskScore(), request.riskBand(), request.decision());
        Transaction saved = transactionRepository.saveAndFlush(transaction);
        auditService.log(AuditAction.TRANSACTION_DECISION_CHANGED, "Transaction", saved.getId(), oldValue,
                Map.of("riskScore", saved.getRiskScore(), "riskBand", saved.getRiskBand(),
                        "decision", saved.getDecision()));
        return TransactionResponse.from(saved);
    }

    @Transactional
    public TransactionResponse updateStatus(
            UUID transactionId,
            UpdateTransactionStatusRequest request
    ) {
        Transaction transaction = findTransaction(transactionId);
        try {
            transaction.transitionTo(request.status());
        } catch (IllegalStateException exception) {
            throw new BadRequestException(exception.getMessage(), exception);
        }
        return TransactionResponse.from(transactionRepository.saveAndFlush(transaction));
    }

    private Transaction findTransaction(UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));
    }
}
