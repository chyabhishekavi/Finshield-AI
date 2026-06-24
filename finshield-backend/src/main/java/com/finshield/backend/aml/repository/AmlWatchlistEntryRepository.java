package com.finshield.backend.aml.repository;

import com.finshield.backend.aml.domain.AmlListType;
import com.finshield.backend.aml.domain.AmlRiskCategory;
import com.finshield.backend.aml.domain.AmlWatchlistEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AmlWatchlistEntryRepository extends JpaRepository<AmlWatchlistEntry, UUID> {

    boolean existsByListTypeAndIdentifier(AmlListType listType, String identifier);

    List<AmlWatchlistEntry> findAllByActiveTrueOrderByNameAsc();

    @Query("""
            select w from AmlWatchlistEntry w
            where (:query is null
                or lower(w.name) like lower(concat('%', :query, '%'))
                or lower(w.identifier) like lower(concat('%', :query, '%')))
              and (:active is null or w.active = :active)
              and (:listType is null or w.listType = :listType)
              and (:riskCategory is null or w.riskCategory = :riskCategory)
            """)
    Page<AmlWatchlistEntry> search(
            @Param("query") String query,
            @Param("active") Boolean active,
            @Param("listType") AmlListType listType,
            @Param("riskCategory") AmlRiskCategory riskCategory,
            Pageable pageable
    );
}
