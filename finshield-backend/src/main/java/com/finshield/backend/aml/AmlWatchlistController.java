package com.finshield.backend.aml;

import com.finshield.backend.aml.api.CreateWatchlistEntryRequest;
import com.finshield.backend.aml.api.UpdateWatchlistEntryRequest;
import com.finshield.backend.aml.api.WatchlistEntryResponse;
import com.finshield.backend.aml.domain.AmlListType;
import com.finshield.backend.aml.domain.AmlRiskCategory;
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
@RequestMapping("/api/aml/watchlist")
@PreAuthorize("hasAnyRole('ADMIN', 'AML_INVESTIGATOR', 'COMPLIANCE_OFFICER', 'RISK_MANAGER')")
public class AmlWatchlistController {

    private final AmlWatchlistService watchlistService;

    public AmlWatchlistController(AmlWatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<WatchlistEntryResponse>> create(
            @Valid @RequestBody CreateWatchlistEntryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("AML watchlist entry created", watchlistService.create(request))
        );
    }

    @GetMapping("/{entryId}")
    public ApiResponse<WatchlistEntryResponse> getById(@PathVariable UUID entryId) {
        return ApiResponse.success(watchlistService.getById(entryId));
    }

    @GetMapping
    public ApiResponse<PageResponse<WatchlistEntryResponse>> search(
            @RequestParam(required = false) @Size(max = 100) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) AmlListType listType,
            @RequestParam(required = false) AmlRiskCategory riskCategory,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(
                watchlistService.search(query, active, listType, riskCategory, page, size));
    }

    @PutMapping("/{entryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER')")
    public ApiResponse<WatchlistEntryResponse> update(
            @PathVariable UUID entryId,
            @Valid @RequestBody UpdateWatchlistEntryRequest request
    ) {
        return ApiResponse.success("AML watchlist entry updated",
                watchlistService.update(entryId, request));
    }

    @DeleteMapping("/{entryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID entryId) {
        watchlistService.deactivate(entryId);
        return ResponseEntity.noContent().build();
    }
}
