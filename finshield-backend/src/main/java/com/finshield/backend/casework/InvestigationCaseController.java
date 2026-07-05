package com.finshield.backend.casework;

import com.finshield.backend.casework.api.*;
import com.finshield.backend.casework.domain.CasePriority;
import com.finshield.backend.casework.domain.InvestigationCaseStatus;
import com.finshield.backend.casework.domain.InvestigationCaseType;
import com.finshield.backend.common.api.ApiResponse;
import com.finshield.backend.common.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/cases")
@PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST', 'AML_INVESTIGATOR', 'COMPLIANCE_OFFICER', 'RISK_MANAGER')")
public class InvestigationCaseController {

    private final InvestigationCaseService service;

    public InvestigationCaseController(InvestigationCaseService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST', 'AML_INVESTIGATOR', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<InvestigationCaseResponse>> create(
            @Valid @RequestBody CreateInvestigationCaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Investigation case created", service.create(request)));
    }

    @GetMapping
    public ApiResponse<PageResponse<InvestigationCaseResponse>> list(
            @RequestParam(required = false) @Size(max = 100) String query,
            @RequestParam(required = false) InvestigationCaseType type,
            @RequestParam(required = false) InvestigationCaseStatus status,
            @RequestParam(required = false) CasePriority priority,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID assignedToId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.search(query, type, status, priority,
                customerId, assignedToId, page, size));
    }

    @GetMapping("/{caseId}")
    public ApiResponse<InvestigationCaseResponse> get(@PathVariable UUID caseId) {
        return ApiResponse.success(service.get(caseId));
    }

    @PatchMapping("/{caseId}/assignment")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST', 'AML_INVESTIGATOR', 'COMPLIANCE_OFFICER')")
    public ApiResponse<InvestigationCaseResponse> assign(@PathVariable UUID caseId,
            @Valid @RequestBody AssignCaseRequest request) {
        return ApiResponse.success("Case assigned", service.assign(caseId, request));
    }

    @PatchMapping("/{caseId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST', 'AML_INVESTIGATOR', 'COMPLIANCE_OFFICER')")
    public ApiResponse<InvestigationCaseResponse> updateStatus(@PathVariable UUID caseId,
            @Valid @RequestBody UpdateCaseStatusRequest request) {
        return ApiResponse.success("Case status updated", service.updateStatus(caseId, request));
    }

    @PatchMapping("/{caseId}/decision")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST', 'AML_INVESTIGATOR', 'COMPLIANCE_OFFICER')")
    public ApiResponse<InvestigationCaseResponse> decision(@PathVariable UUID caseId,
            @Valid @RequestBody RecordCaseDecisionRequest request) {
        return ApiResponse.success("Case decision recorded", service.recordDecision(caseId, request));
    }

    @PatchMapping("/{caseId}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST', 'AML_INVESTIGATOR', 'COMPLIANCE_OFFICER')")
    public ApiResponse<InvestigationCaseResponse> close(@PathVariable UUID caseId,
            @Valid @RequestBody RecordCaseDecisionRequest request) {
        return ApiResponse.success("Case closed", service.close(caseId, request));
    }

    @PostMapping("/{caseId}/notes")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST', 'AML_INVESTIGATOR', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<CaseNoteResponse>> addNote(@PathVariable UUID caseId,
            @Valid @RequestBody AddCaseNoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Case note added", service.addNote(caseId, request)));
    }

    @GetMapping("/{caseId}/notes")
    public ApiResponse<List<CaseNoteResponse>> listNotes(@PathVariable UUID caseId) {
        return ApiResponse.success(service.listNotes(caseId));
    }

    @PostMapping("/{caseId}/evidence")
    @PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST', 'AML_INVESTIGATOR', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<CaseEvidenceResponse>> addEvidence(@PathVariable UUID caseId,
            @Valid @RequestBody AddCaseEvidenceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Evidence metadata added", service.addEvidence(caseId, request)));
    }

    @GetMapping("/{caseId}/evidence")
    public ApiResponse<List<CaseEvidenceResponse>> listEvidence(@PathVariable UUID caseId) {
        return ApiResponse.success(service.listEvidence(caseId));
    }

    @GetMapping("/{caseId}/status-history")
    public ApiResponse<List<CaseStatusHistoryResponse>> statusHistory(@PathVariable UUID caseId) {
        return ApiResponse.success(service.listStatusHistory(caseId));
    }
}
