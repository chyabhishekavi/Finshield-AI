package com.finshield.backend.beneficiary;

import com.finshield.backend.beneficiary.api.AddBeneficiaryRequest;
import com.finshield.backend.beneficiary.api.BeneficiaryResponse;
import com.finshield.backend.beneficiary.api.UpdateBeneficiaryRequest;
import com.finshield.backend.common.api.ApiResponse;
import com.finshield.backend.common.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/customers/{customerId}/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST', 'AML_INVESTIGATOR', 'RISK_MANAGER')")
    public ResponseEntity<ApiResponse<BeneficiaryResponse>> add(
            @PathVariable UUID customerId,
            @Valid @RequestBody AddBeneficiaryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Beneficiary added successfully", beneficiaryService.add(customerId, request))
        );
    }

    @GetMapping
    public ApiResponse<PageResponse<BeneficiaryResponse>> list(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(beneficiaryService.list(customerId, page, size));
    }

    @PatchMapping("/{beneficiaryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST', 'AML_INVESTIGATOR', 'RISK_MANAGER')")
    public ApiResponse<BeneficiaryResponse> update(
            @PathVariable UUID customerId,
            @PathVariable UUID beneficiaryId,
            @Valid @RequestBody UpdateBeneficiaryRequest request
    ) {
        return ApiResponse.success(
                "Beneficiary updated successfully",
                beneficiaryService.update(customerId, beneficiaryId, request)
        );
    }
}
