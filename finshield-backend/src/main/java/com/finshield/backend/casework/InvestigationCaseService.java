package com.finshield.backend.casework;

import com.finshield.backend.auth.security.CurrentUser;
import com.finshield.backend.audit.AuditService;
import com.finshield.backend.audit.domain.AuditAction;
import com.finshield.backend.casework.api.*;
import com.finshield.backend.casework.domain.*;
import com.finshield.backend.casework.repository.*;
import com.finshield.backend.common.api.PageResponse;
import com.finshield.backend.common.exception.BadRequestException;
import com.finshield.backend.common.exception.ResourceNotFoundException;
import com.finshield.backend.fraud.alert.domain.FraudAlert;
import com.finshield.backend.fraud.alert.repository.FraudAlertRepository;
import com.finshield.backend.notification.NotificationService;
import com.finshield.backend.transaction.domain.Transaction;
import com.finshield.backend.transaction.repository.TransactionRepository;
import com.finshield.backend.user.domain.RoleName;
import com.finshield.backend.user.domain.User;
import com.finshield.backend.user.domain.UserStatus;
import com.finshield.backend.user.repository.UserRepository;
import com.finshield.backend.user.repository.UserRoleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class InvestigationCaseService {

    private static final DateTimeFormatter CASE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
    private final InvestigationCaseRepository caseRepository;
    private final CaseNoteRepository noteRepository;
    private final CaseEvidenceRepository evidenceRepository;
    private final FraudAlertRepository alertRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final CurrentUser currentUser;
    private final CaseStatusHistoryRepository statusHistoryRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public InvestigationCaseService(InvestigationCaseRepository caseRepository,
            CaseNoteRepository noteRepository, CaseEvidenceRepository evidenceRepository,
            FraudAlertRepository alertRepository, TransactionRepository transactionRepository,
            UserRepository userRepository, UserRoleRepository userRoleRepository, CurrentUser currentUser,
            CaseStatusHistoryRepository statusHistoryRepository, AuditService auditService,
            NotificationService notificationService) {
        this.caseRepository = caseRepository;
        this.noteRepository = noteRepository;
        this.evidenceRepository = evidenceRepository;
        this.alertRepository = alertRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.currentUser = currentUser;
        this.statusHistoryRepository = statusHistoryRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional
    public InvestigationCaseResponse create(CreateInvestigationCaseRequest request) {
        Transaction transaction = transactionRepository.findById(request.primaryTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", request.primaryTransactionId()));
        FraudAlert alert = request.linkedAlertId() == null ? null : alertRepository.findById(request.linkedAlertId())
                .orElseThrow(() -> new ResourceNotFoundException("Fraud alert", request.linkedAlertId()));
        validateLink(transaction, alert);
        if (alert != null && caseRepository.findByLinkedAlertId(alert.getId()).isPresent()) {
            throw new BadRequestException("The fraud alert is already linked to a case");
        }
        InvestigationCase value = new InvestigationCase(nextCaseNumber(), request.caseType(),
                transaction.getSourceAccount().getCustomer(), transaction, alert,
                request.priority(), request.summary());
        if (alert != null) mutate(() -> alert.convertToCase("Linked to " + value.getCaseNumber()));
        return InvestigationCaseResponse.from(caseRepository.saveAndFlush(value));
    }

    @Transactional
    public InvestigationCase createAutomaticCriticalCase(FraudAlert alert) {
        InvestigationCase existing = caseRepository.findByLinkedAlertId(alert.getId()).orElse(null);
        if (existing != null) return existing;
        InvestigationCase value = new InvestigationCase(nextCaseNumber(), inferType(alert), alert.getCustomer(),
                alert.getTransaction(), alert, CasePriority.CRITICAL,
                "Automatically opened from critical fraud alert " + alert.getAlertNumber() + ". " + alert.getAlertSummary());
        alert.convertToCase("Automatic critical case " + value.getCaseNumber());
        return caseRepository.saveAndFlush(value);
    }

    @Transactional(readOnly = true)
    public PageResponse<InvestigationCaseResponse> search(String query, InvestigationCaseType type,
            InvestigationCaseStatus status, CasePriority priority, UUID customerId,
            UUID assignedToId, int page, int size) {
        String q = query == null || query.isBlank() ? "" : query.trim();
        return PageResponse.from(caseRepository.search(q, type, status, priority, customerId, assignedToId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))),
                InvestigationCaseResponse::from);
    }

    @Transactional(readOnly = true)
    public InvestigationCaseResponse get(UUID caseId) {
        return InvestigationCaseResponse.from(findCase(caseId));
    }

    @Transactional
    public InvestigationCaseResponse assign(UUID caseId, AssignCaseRequest request) {
        InvestigationCase value = findCase(caseId);
        User investigator = findEligibleInvestigator(request.assignedTo());
        InvestigationCaseStatus oldStatus = value.getStatus();
        mutate(() -> value.assign(investigator));
        InvestigationCaseResponse response = save(value);
        recordStatusChange(value, oldStatus);
        notificationService.notifyCaseAssigned(value, investigator);
        return response;
    }

    @Transactional
    public InvestigationCaseResponse updateStatus(UUID caseId, UpdateCaseStatusRequest request) {
        InvestigationCase value = findCase(caseId);
        InvestigationCaseStatus oldStatus = value.getStatus();
        mutate(() -> value.changeStatus(request.status()));
        InvestigationCaseResponse response = save(value);
        recordStatusChange(value, oldStatus);
        if (value.getStatus() == InvestigationCaseStatus.ESCALATED_TO_COMPLIANCE) {
            notificationService.notifyCaseEscalated(value);
        }
        return response;
    }

    @Transactional
    public InvestigationCaseResponse recordDecision(UUID caseId, RecordCaseDecisionRequest request) {
        InvestigationCase value = findCase(caseId);
        mutate(() -> value.recordDecision(request.decision(), request.rationale()));
        return save(value);
    }

    @Transactional
    public InvestigationCaseResponse close(UUID caseId, RecordCaseDecisionRequest request) {
        InvestigationCase value = findCase(caseId);
        InvestigationCaseStatus oldStatus = value.getStatus();
        mutate(() -> value.close(request.decision(), request.rationale(), Instant.now()));
        InvestigationCaseResponse response = save(value);
        recordStatusChange(value, oldStatus);
        return response;
    }

    @Transactional
    public CaseNoteResponse addNote(UUID caseId, AddCaseNoteRequest request) {
        InvestigationCase value = findCase(caseId);
        ensureOpen(value);
        User author = currentUserEntity();
        return CaseNoteResponse.from(noteRepository.saveAndFlush(
                new CaseNote(value, request.noteText(), author, request.internalOnly())));
    }

    @Transactional(readOnly = true)
    public List<CaseNoteResponse> listNotes(UUID caseId) {
        findCase(caseId);
        return noteRepository.findAllByInvestigationCaseIdOrderByCreatedAtAsc(caseId).stream()
                .map(CaseNoteResponse::from).toList();
    }

    @Transactional
    public CaseEvidenceResponse addEvidence(UUID caseId, AddCaseEvidenceRequest request) {
        InvestigationCase value = findCase(caseId);
        ensureOpen(value);
        User uploader = currentUserEntity();
        return CaseEvidenceResponse.from(evidenceRepository.saveAndFlush(new CaseEvidence(value,
                request.evidenceType(), request.fileName(), request.fileUrl(), request.description(),
                uploader, Instant.now())));
    }

    @Transactional(readOnly = true)
    public List<CaseEvidenceResponse> listEvidence(UUID caseId) {
        findCase(caseId);
        return evidenceRepository.findAllByInvestigationCaseIdOrderByUploadedAtAsc(caseId).stream()
                .map(CaseEvidenceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CaseStatusHistoryResponse> listStatusHistory(UUID caseId) {
        findCase(caseId);
        return statusHistoryRepository.findAllByInvestigationCaseIdOrderByCreatedAtAsc(caseId).stream()
                .map(CaseStatusHistoryResponse::from).toList();
    }

    private void recordStatusChange(InvestigationCase value, InvestigationCaseStatus oldStatus) {
        if (oldStatus == value.getStatus()) return;
        UUID actorId = currentUser.id();
        statusHistoryRepository.save(new CaseStatusHistory(value, oldStatus, value.getStatus(), actorId));
        AuditAction action = value.getStatus() == InvestigationCaseStatus.ESCALATED_TO_COMPLIANCE
                ? AuditAction.CASE_ESCALATED : AuditAction.CASE_STATUS_CHANGED;
        auditService.log(action, "InvestigationCase", value.getId(),
                Map.of("status", oldStatus), Map.of("status", value.getStatus()));
    }

    private void validateLink(Transaction transaction, FraudAlert alert) {
        if (alert != null && (!alert.getTransaction().getId().equals(transaction.getId())
                || !alert.getCustomer().getId().equals(transaction.getSourceAccount().getCustomer().getId()))) {
            throw new BadRequestException("Linked alert, transaction, and customer must belong to the same event");
        }
    }

    private InvestigationCaseType inferType(FraudAlert alert) {
        String summary = alert.getAlertSummary().toUpperCase(Locale.ROOT);
        if (summary.contains("MULE_ACCOUNT_BEHAVIOR")) return InvestigationCaseType.MULE_ACCOUNT;
        if (summary.contains("SANCTIONS")) return InvestigationCaseType.SANCTIONS_MATCH;
        if (summary.contains("AML")) return InvestigationCaseType.AML;
        return InvestigationCaseType.FRAUD;
    }

    private User findEligibleInvestigator(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (user.getStatus() != UserStatus.ACTIVE) throw new BadRequestException("Case assignee must be active");
        boolean eligible = userRoleRepository.existsByUserIdAndRoleName(userId, RoleName.ADMIN)
                || userRoleRepository.existsByUserIdAndRoleName(userId, RoleName.FRAUD_ANALYST)
                || userRoleRepository.existsByUserIdAndRoleName(userId, RoleName.AML_INVESTIGATOR)
                || userRoleRepository.existsByUserIdAndRoleName(userId, RoleName.COMPLIANCE_OFFICER);
        if (!eligible) throw new BadRequestException("User does not have an investigation role");
        return user;
    }

    private User currentUserEntity() {
        UUID id = currentUser.id();
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private InvestigationCase findCase(UUID id) {
        return caseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Investigation case", id));
    }

    private void ensureOpen(InvestigationCase value) {
        if (value.getStatus() == InvestigationCaseStatus.CLOSED)
            throw new BadRequestException("Notes and evidence cannot be added to a closed case");
    }

    private InvestigationCaseResponse save(InvestigationCase value) {
        return InvestigationCaseResponse.from(caseRepository.saveAndFlush(value));
    }

    private void mutate(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException | IllegalStateException exception) {
            throw new BadRequestException(exception.getMessage(), exception);
        }
    }

    private String nextCaseNumber() {
        return "CASE-" + CASE_DATE.format(Instant.now()) + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }
}
