package com.finshield.backend.dashboard;

import com.finshield.backend.common.api.ApiResponse;
import com.finshield.backend.dashboard.api.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST', 'AML_INVESTIGATOR', 'COMPLIANCE_OFFICER', 'RISK_MANAGER')")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) { this.service = service; }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> summary() {
        return ApiResponse.success(service.summary());
    }

    @GetMapping("/risk-trends")
    public ApiResponse<RiskTrendResponse> riskTrends(
            @RequestParam(defaultValue = "30") @Min(1) @Max(90) int days) {
        return ApiResponse.success(service.riskTrends(days));
    }

    @GetMapping("/top-rules")
    public ApiResponse<TopRulesResponse> topRules(
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return ApiResponse.success(service.topRules(days, limit));
    }

    @GetMapping("/alert-status-count")
    public ApiResponse<AlertStatusCountResponse> alertStatusCounts() {
        return ApiResponse.success(service.alertStatusCounts());
    }

    @GetMapping("/high-risk-transactions")
    public ApiResponse<HighRiskTransactionsResponse> highRiskTransactions(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return ApiResponse.success(service.highRiskTransactions(limit));
    }

    @GetMapping("/case-sla-summary")
    public ApiResponse<CaseSlaSummaryResponse> caseSlaSummary(
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        return ApiResponse.success(service.caseSlaSummary(days));
    }
}
