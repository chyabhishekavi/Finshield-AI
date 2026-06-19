package com.finshield.backend.fraud.alert;

import com.finshield.backend.common.api.ApiResponse;
import com.finshield.backend.common.api.PageResponse;
import com.finshield.backend.fraud.alert.api.AssignFraudAlertRequest;
import com.finshield.backend.fraud.alert.api.CloseFraudAlertRequest;
import com.finshield.backend.fraud.alert.api.EscalateFraudAlertRequest;
import com.finshield.backend.fraud.alert.api.FraudAlertResponse;
import com.finshield.backend.fraud.alert.api.UpdateFraudAlertStatusRequest;
import com.finshield.backend.fraud.alert.api.AlertAssigneeResponse;
import com.finshield.backend.fraud.alert.domain.FraudAlertSeverity;
import com.finshield.backend.fraud.alert.domain.FraudAlertStatus;
import com.finshield.backend.transaction.domain.RiskBand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Sort;

@Validated
@RestController
@RequestMapping("/api/fraud/alerts")
@PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST', 'AML_INVESTIGATOR', 'COMPLIANCE_OFFICER', 'RISK_MANAGER')")
public class FraudAlertController {

    private final FraudAlertService alertService;

    public FraudAlertController(FraudAlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ApiResponse<PageResponse<FraudAlertResponse>> list(
            @RequestParam(required = false) @Size(max = 100) String query,
            @RequestParam(required = false) FraudAlertStatus status,
            @RequestParam(required = false) FraudAlertSeverity severity,
            @RequestParam(required = false) RiskBand riskBand,
            @RequestParam(required = false) UUID assignedToId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(defaultValue = "dueAt") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction sortDirection,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(alertService.search(query, status, severity, riskBand,
                assignedToId, customerId, overdue, sortBy, sortDirection, page, size));
    }

    @GetMapping("/assignees")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST', 'RISK_MANAGER')")
    public ApiResponse<List<AlertAssigneeResponse>> assignees() {
        return ApiResponse.success(alertService.eligibleAssignees());
    }

    @GetMapping("/{alertId}")
    public ApiResponse<FraudAlertResponse> getDetails(@PathVariable UUID alertId) {
        return ApiResponse.success(alertService.getById(alertId));
    }

    @PatchMapping("/{alertId}/assignment")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST', 'RISK_MANAGER')")
    public ApiResponse<FraudAlertResponse> assign(
            @PathVariable UUID alertId,
            @Valid @RequestBody AssignFraudAlertRequest request
    ) {
        return ApiResponse.success("Fraud alert assigned", alertService.assign(alertId, request));
    }

    @PatchMapping("/{alertId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST', 'RISK_MANAGER')")
    public ApiResponse<FraudAlertResponse> updateStatus(
            @PathVariable UUID alertId,
            @Valid @RequestBody UpdateFraudAlertStatusRequest request
    ) {
        return ApiResponse.success("Fraud alert status updated",
                alertService.updateStatus(alertId, request));
    }

    @PatchMapping("/{alertId}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST', 'RISK_MANAGER')")
    public ApiResponse<FraudAlertResponse> close(
            @PathVariable UUID alertId,
            @Valid @RequestBody CloseFraudAlertRequest request
    ) {
        return ApiResponse.success("Fraud alert closed", alertService.close(alertId, request));
    }

    @PatchMapping("/{alertId}/escalate")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST', 'RISK_MANAGER')")
    public ApiResponse<FraudAlertResponse> escalate(
            @PathVariable UUID alertId,
            @Valid @RequestBody EscalateFraudAlertRequest request
    ) {
        return ApiResponse.success("Fraud alert escalated", alertService.escalate(alertId, request));
    }
}
