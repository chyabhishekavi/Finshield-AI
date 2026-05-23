package com.finshield.backend.device;

import com.finshield.backend.common.api.ApiResponse;
import com.finshield.backend.common.api.PageResponse;
import com.finshield.backend.device.api.AddCustomerDeviceRequest;
import com.finshield.backend.device.api.CustomerDeviceResponse;
import com.finshield.backend.device.api.UpdateCustomerDeviceRequest;
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
@RequestMapping("/api/customers/{customerId}/devices")
public class CustomerDeviceController {

    private final CustomerDeviceService deviceService;

    public CustomerDeviceController(CustomerDeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST', 'RISK_MANAGER')")
    public ResponseEntity<ApiResponse<CustomerDeviceResponse>> add(
            @PathVariable UUID customerId,
            @Valid @RequestBody AddCustomerDeviceRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Customer device added successfully", deviceService.add(customerId, request))
        );
    }

    @GetMapping
    public ApiResponse<PageResponse<CustomerDeviceResponse>> list(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(deviceService.list(customerId, page, size));
    }

    @PatchMapping("/{deviceRecordId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST', 'RISK_MANAGER')")
    public ApiResponse<CustomerDeviceResponse> update(
            @PathVariable UUID customerId,
            @PathVariable UUID deviceRecordId,
            @Valid @RequestBody UpdateCustomerDeviceRequest request
    ) {
        return ApiResponse.success(
                "Customer device updated successfully",
                deviceService.update(customerId, deviceRecordId, request)
        );
    }
}
