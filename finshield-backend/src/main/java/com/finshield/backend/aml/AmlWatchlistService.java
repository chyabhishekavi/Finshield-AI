package com.finshield.backend.aml;

import com.finshield.backend.aml.api.CreateWatchlistEntryRequest;
import com.finshield.backend.aml.api.UpdateWatchlistEntryRequest;
import com.finshield.backend.aml.api.WatchlistEntryResponse;
import com.finshield.backend.aml.domain.AmlListType;
import com.finshield.backend.aml.domain.AmlRiskCategory;
import com.finshield.backend.aml.domain.AmlWatchlistEntry;
import com.finshield.backend.aml.repository.AmlWatchlistEntryRepository;
import com.finshield.backend.common.api.PageResponse;
import com.finshield.backend.common.exception.BadRequestException;
import com.finshield.backend.common.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class AmlWatchlistService {

    private final AmlWatchlistEntryRepository repository;

    public AmlWatchlistService(AmlWatchlistEntryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public WatchlistEntryResponse create(CreateWatchlistEntryRequest request) {
        String identifier = request.identifier().trim().toUpperCase(Locale.ROOT);
        if (repository.existsByListTypeAndIdentifier(request.listType(), identifier)) {
            throw new BadRequestException("A watchlist entry with this list type and identifier already exists");
        }
        try {
            return WatchlistEntryResponse.from(repository.saveAndFlush(new AmlWatchlistEntry(
                    request.name(), identifier, request.country(), request.listType(),
                    request.riskCategory(), request.active()
            )));
        } catch (DataIntegrityViolationException exception) {
            throw new BadRequestException(
                    "A watchlist entry with this list type and identifier already exists", exception);
        }
    }

    @Transactional(readOnly = true)
    public WatchlistEntryResponse getById(UUID entryId) {
        return WatchlistEntryResponse.from(findEntry(entryId));
    }

    @Transactional(readOnly = true)
    public PageResponse<WatchlistEntryResponse> search(
            String query,
            Boolean active,
            AmlListType listType,
            AmlRiskCategory riskCategory,
            int page,
            int size
    ) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        return PageResponse.from(repository.search(
                normalizedQuery, active, listType, riskCategory,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"))
        ), WatchlistEntryResponse::from);
    }

    @Transactional
    public WatchlistEntryResponse update(UUID entryId, UpdateWatchlistEntryRequest request) {
        AmlWatchlistEntry entry = findEntry(entryId);
        entry.update(request.name(), request.country(), request.listType(),
                request.riskCategory(), request.active());
        try {
            return WatchlistEntryResponse.from(repository.saveAndFlush(entry));
        } catch (DataIntegrityViolationException exception) {
            throw new BadRequestException(
                    "A watchlist entry with this list type and identifier already exists", exception);
        }
    }

    @Transactional
    public void deactivate(UUID entryId) {
        AmlWatchlistEntry entry = findEntry(entryId);
        entry.deactivate();
        repository.saveAndFlush(entry);
    }

    private AmlWatchlistEntry findEntry(UUID entryId) {
        return repository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("AML watchlist entry", entryId));
    }
}
