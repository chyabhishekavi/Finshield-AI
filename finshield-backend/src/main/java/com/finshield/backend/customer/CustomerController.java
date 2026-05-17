package com.finshield.backend.customer;

import com.finshield.backend.common.api.ApiResponse;
import com.finshield.backend.common.api.PageResponse;
import com.finshield.backend.customer.api.CreateCustomerRequest;
import com.finshield.backend.customer.api.Customer360Response;
import com.finshield.backend.customer.api.CustomerResponse;
import com.finshield.backend.customer.api.CustomerSummaryResponse;
import com.finshield.backend.customer.api.UpdateCustomerRiskRequest;
import com.finshield.backend.customer.domain.CustomerRiskLevel;
import com.finshield.backend.customer.domain.KycStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<CustomerResponse>> create(
            @Valid @RequestBody CreateCustomerRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Customer created successfully", customerService.create(request))
        );
    }

    @GetMapping("/{customerId}")
    public ApiResponse<CustomerResponse> getById(@PathVariable UUID customerId) {
        return ApiResponse.success(customerService.getById(customerId));
    }

    @GetMapping
    public ApiResponse<PageResponse<CustomerSummaryResponse>> search(
            @RequestParam(required = false) @Size(max = 100) String query,
            @RequestParam(required = false) KycStatus kycStatus,
            @RequestParam(required = false) CustomerRiskLevel riskLevel,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(customerService.search(query, kycStatus, riskLevel, page, size));
    }

    @GetMapping("/{customerId}/360")
    public ApiResponse<Customer360Response> get360Profile(@PathVariable UUID customerId) {
        return ApiResponse.success(customerService.get360Profile(customerId));
    }

    @PatchMapping("/{customerId}/risk-level")
    @PreAuthorize("hasAnyRole('ADMIN', 'RISK_MANAGER', 'COMPLIANCE_OFFICER')")
    public ApiResponse<CustomerResponse> updateRiskLevel(
            @PathVariable UUID customerId,
            @Valid @RequestBody UpdateCustomerRiskRequest request
    ) {
        return ApiResponse.success(
                "Customer risk level updated successfully",
                customerService.updateRiskLevel(customerId, request)
        );
    }
}
