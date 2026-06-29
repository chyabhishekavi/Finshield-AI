package com.finshield.backend.aml;

import com.finshield.backend.aml.api.AmlScreeningResponse;
import com.finshield.backend.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/aml/screenings")
@PreAuthorize("hasAnyRole('ADMIN', 'AML_INVESTIGATOR', 'COMPLIANCE_OFFICER', 'RISK_MANAGER')")
public class AmlScreeningController {

    private final AmlScreeningService screeningService;

    public AmlScreeningController(AmlScreeningService screeningService) {
        this.screeningService = screeningService;
    }

    @PostMapping("/customers/{customerId}")
    public ApiResponse<AmlScreeningResponse> screenCustomer(@PathVariable UUID customerId) {
        return ApiResponse.success("Customer AML screening completed",
                screeningService.screenCustomer(customerId));
    }

    @PostMapping("/beneficiaries/{beneficiaryId}")
    public ApiResponse<AmlScreeningResponse> screenBeneficiary(@PathVariable UUID beneficiaryId) {
        return ApiResponse.success("Beneficiary AML screening completed",
                screeningService.screenBeneficiary(beneficiaryId));
    }
}
