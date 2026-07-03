package com.finshield.backend.casework.domain;

import com.finshield.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Entity
@Table(name = "case_status_history", indexes = @Index(
        name = "idx_case_status_history_case_created", columnList = "case_id, created_at"))
public class CaseStatusHistory extends BaseEntity {
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_case_status_history_case"))
    private InvestigationCase investigationCase;
    @NotNull @Enumerated(EnumType.STRING) @Column(name = "old_status", nullable = false, updatable = false, length = 40)
    private InvestigationCaseStatus oldStatus;
    @NotNull @Enumerated(EnumType.STRING) @Column(name = "new_status", nullable = false, updatable = false, length = 40)
    private InvestigationCaseStatus newStatus;
    @Column(name = "changed_by_user_id", updatable = false) private UUID changedByUserId;

    protected CaseStatusHistory() {}
    public CaseStatusHistory(InvestigationCase value, InvestigationCaseStatus oldStatus,
            InvestigationCaseStatus newStatus, UUID changedByUserId) {
        this.investigationCase = value;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedByUserId = changedByUserId;
    }
    public InvestigationCase getInvestigationCase() { return investigationCase; }
    public InvestigationCaseStatus getOldStatus() { return oldStatus; }
    public InvestigationCaseStatus getNewStatus() { return newStatus; }
    public UUID getChangedByUserId() { return changedByUserId; }
}
