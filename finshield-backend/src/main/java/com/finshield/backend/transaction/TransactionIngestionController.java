package com.finshield.backend.transaction;

import com.finshield.backend.common.api.ApiResponse;
import com.finshield.backend.transaction.api.TransactionIngestRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionIngestionController {

    private final TransactionIngestionService ingestionService;

    public TransactionIngestionController(TransactionIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST', 'RISK_MANAGER')")
    public ResponseEntity<ApiResponse<String>> ingest(
            @Valid @RequestBody TransactionIngestRequest request
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                ApiResponse.success("Transaction accepted for risk processing", ingestionService.ingest(request))
        );
    }
}
