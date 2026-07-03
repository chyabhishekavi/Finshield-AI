package com.finshield.backend.casework.domain;

import com.finshield.backend.common.entity.BaseEntity;
import com.finshield.backend.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Objects;

@Entity
@Table(name = "case_notes", indexes = @Index(name = "idx_case_notes_case_created", columnList = "case_id, created_at"))
public class CaseNote extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_case_note_case"))
    private InvestigationCase investigationCase;

    @NotBlank
    @Size(max = 4000)
    @Column(name = "note_text", nullable = false, updatable = false, length = 4000)
    private String noteText;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_case_note_created_by"))
    private User createdBy;

    @Column(name = "internal_only", nullable = false, updatable = false)
    private boolean internalOnly;

    protected CaseNote() {
    }

    public CaseNote(InvestigationCase investigationCase, String noteText, User createdBy, boolean internalOnly) {
        this.investigationCase = Objects.requireNonNull(investigationCase);
        this.noteText = Objects.requireNonNull(noteText).trim();
        this.createdBy = Objects.requireNonNull(createdBy);
        this.internalOnly = internalOnly;
    }

    public InvestigationCase getInvestigationCase() { return investigationCase; }
    public String getNoteText() { return noteText; }
    public User getCreatedBy() { return createdBy; }
    public boolean isInternalOnly() { return internalOnly; }
}
