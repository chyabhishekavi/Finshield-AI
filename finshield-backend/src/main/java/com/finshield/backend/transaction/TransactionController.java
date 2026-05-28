package com.finshield.backend.transaction;

import com.finshield.backend.common.api.ApiResponse;
import com.finshield.backend.common.api.PageResponse;
import com.finshield.backend.transaction.api.ApplyRiskDecisionRequest;
import com.finshield.backend.transaction.api.CreateTransactionRequest;
import com.finshield.backend.transaction.api.TransactionResponse;
import com.finshield.backend.transaction.api.TransactionRiskExplanationResponse;
import com.finshield.backend.transaction.api.UpdateTransactionStatusRequest;
import com.finshield.backend.transaction.domain.RiskBand;
import com.finshield.backend.transaction.domain.TransactionStatus;
import com.finshield.backend.transaction.domain.TransactionType;
import com.finshield.backend.transaction.domain.TransactionChannel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST', 'RISK_MANAGER')")
    public ResponseEntity<ApiResponse<TransactionResponse>> create(
            @Valid @RequestBody CreateTransactionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Transaction created successfully", transactionService.create(request))
        );
    }

    @GetMapping("/{transactionId}")
    public ApiResponse<TransactionResponse> getById(@PathVariable UUID transactionId) {
        return ApiResponse.success(transactionService.getById(transactionId));
    }

    @GetMapping("/reference/{reference}")
    public ApiResponse<TransactionResponse> getByReference(
            @PathVariable @Size(min = 6, max = 64) String reference
    ) {
        return ApiResponse.success(transactionService.getByReference(reference));
    }

    @GetMapping
    public ApiResponse<PageResponse<TransactionResponse>> search(
            @RequestParam(required = false) UUID sourceAccountId,
            @RequestParam(required = false) @Size(max = 150) String query,
            @RequestParam(required = false) @Size(max = 64) String reference,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) RiskBand riskBand,
            @RequestParam(required = false) TransactionChannel channel,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal minAmount,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal maxAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toTime,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(transactionService.search(
                sourceAccountId,
                query == null || query.isBlank() ? reference : query,
                status, type, riskBand, channel, minAmount, maxAmount,
                fromTime, toTime, page, size));
    }

    @GetMapping("/{transactionId}/risk-explanation")
    public ApiResponse<TransactionRiskExplanationResponse> riskExplanation(
            @PathVariable UUID transactionId
    ) {
        return ApiResponse.success(transactionService.riskExplanation(transactionId));
    }

    @PatchMapping("/{transactionId}/risk-decision")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST', 'RISK_MANAGER')")
    public ApiResponse<TransactionResponse> applyRiskDecision(
            @PathVariable UUID transactionId,
            @Valid @RequestBody ApplyRiskDecisionRequest request
    ) {
        return ApiResponse.success(
                "Transaction risk decision applied successfully",
                transactionService.applyRiskDecision(transactionId, request)
        );
    }

    @PatchMapping("/{transactionId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST', 'RISK_MANAGER')")
    public ApiResponse<TransactionResponse> updateStatus(
            @PathVariable UUID transactionId,
            @Valid @RequestBody UpdateTransactionStatusRequest request
    ) {
        return ApiResponse.success(
                "Transaction status updated successfully",
                transactionService.updateStatus(transactionId, request)
        );
    }
}
