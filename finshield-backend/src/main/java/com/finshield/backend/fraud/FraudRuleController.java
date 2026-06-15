package com.finshield.backend.fraud;

import com.finshield.backend.common.api.ApiResponse;
import com.finshield.backend.common.api.PageResponse;
import com.finshield.backend.fraud.api.CreateFraudRuleRequest;
import com.finshield.backend.fraud.api.FraudRuleResponse;
import com.finshield.backend.fraud.api.UpdateFraudRuleRequest;
import com.finshield.backend.fraud.domain.FraudRuleType;
import com.finshield.backend.fraud.domain.RuleSeverity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/fraud/rules")
@PreAuthorize("hasAnyRole('ADMIN', 'RISK_MANAGER')")
public class FraudRuleController {

    private final FraudRuleService ruleService;

    public FraudRuleController(FraudRuleService ruleService) {
        this.ruleService = ruleService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FraudRuleResponse>> create(
            @Valid @RequestBody CreateFraudRuleRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Fraud rule created successfully", ruleService.create(request))
        );
    }

    @GetMapping("/{ruleId}")
    public ApiResponse<FraudRuleResponse> getById(@PathVariable UUID ruleId) {
        return ApiResponse.success(ruleService.getById(ruleId));
    }

    @GetMapping
    public ApiResponse<PageResponse<FraudRuleResponse>> search(
            @RequestParam(required = false) @Size(max = 100) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) FraudRuleType ruleType,
            @RequestParam(required = false) RuleSeverity severity,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(ruleService.search(query, active, ruleType, severity, page, size));
    }

    @PutMapping("/{ruleId}")
    public ApiResponse<FraudRuleResponse> update(
            @PathVariable UUID ruleId,
            @Valid @RequestBody UpdateFraudRuleRequest request
    ) {
        return ApiResponse.success(
                "Fraud rule updated successfully",
                ruleService.update(ruleId, request)
        );
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID ruleId) {
        ruleService.deactivate(ruleId);
        return ResponseEntity.noContent().build();
    }
}
