package com.finshield.backend.casework.domain;

import com.finshield.backend.common.entity.BaseEntity;
import com.finshield.backend.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "case_evidence", indexes = @Index(name = "idx_case_evidence_case_uploaded", columnList = "case_id, uploaded_at"))
public class CaseEvidence extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_case_evidence_case"))
    private InvestigationCase investigationCase;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false, updatable = false, length = 40)
    private EvidenceType evidenceType;

    @NotBlank
    @Size(max = 255)
    @Column(name = "file_name", nullable = false, updatable = false, length = 255)
    private String fileName;

    @NotBlank
    @Size(max = 1000)
    @Column(name = "file_url", nullable = false, updatable = false, length = 1000)
    private String fileUrl;

    @Size(max = 2000)
    @Column(updatable = false, length = 2000)
    private String description;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_id", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_case_evidence_uploaded_by"))
    private User uploadedBy;

    @NotNull
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    protected CaseEvidence() {
    }

    public CaseEvidence(InvestigationCase investigationCase, EvidenceType evidenceType,
            String fileName, String fileUrl, String description, User uploadedBy, Instant uploadedAt) {
        this.investigationCase = Objects.requireNonNull(investigationCase);
        this.evidenceType = Objects.requireNonNull(evidenceType);
        this.fileName = Objects.requireNonNull(fileName).trim();
        this.fileUrl = Objects.requireNonNull(fileUrl).trim();
        this.description = description == null || description.isBlank() ? null : description.trim();
        this.uploadedBy = Objects.requireNonNull(uploadedBy);
        this.uploadedAt = Objects.requireNonNull(uploadedAt);
    }

    public InvestigationCase getInvestigationCase() { return investigationCase; }
    public EvidenceType getEvidenceType() { return evidenceType; }
    public String getFileName() { return fileName; }
    public String getFileUrl() { return fileUrl; }
    public String getDescription() { return description; }
    public User getUploadedBy() { return uploadedBy; }
    public Instant getUploadedAt() { return uploadedAt; }
}
