package com.finshield.backend.notification;

import com.finshield.backend.auth.security.CurrentUser;
import com.finshield.backend.casework.domain.InvestigationCase;
import com.finshield.backend.common.api.PageResponse;
import com.finshield.backend.common.exception.ResourceNotFoundException;
import com.finshield.backend.fraud.domain.FraudRule;
import com.finshield.backend.fraud.alert.domain.FraudAlert;
import com.finshield.backend.notification.api.NotificationResponse;
import com.finshield.backend.notification.domain.Notification;
import com.finshield.backend.notification.domain.NotificationType;
import com.finshield.backend.notification.repository.NotificationRepository;
import com.finshield.backend.notification.websocket.NotificationCreatedEvent;
import com.finshield.backend.user.domain.RoleName;
import com.finshield.backend.user.domain.User;
import com.finshield.backend.user.domain.UserStatus;
import com.finshield.backend.user.repository.UserRoleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.UUID;

@Service
public class NotificationService {
    private final NotificationRepository repository;
    private final UserRoleRepository userRoleRepository;
    private final CurrentUser currentUser;
    private final ApplicationEventPublisher eventPublisher;

    public NotificationService(NotificationRepository repository,
            UserRoleRepository userRoleRepository, CurrentUser currentUser,
            ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.userRoleRepository = userRoleRepository;
        this.currentUser = currentUser;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void notifyAlertCreated(FraudAlert alert) {
        notifyRoles(Set.of(RoleName.FRAUD_ANALYST, RoleName.RISK_MANAGER), NotificationType.ALERT_CREATED,
                "Fraud alert " + alert.getAlertNumber(),
                "A fraud alert was created for transaction " + alert.getTransaction().getTransactionReference(),
                alert.getId());
    }

    @Transactional
    public void createUrgentFraudAlertNotification(FraudAlert alert) {
        notifyRoles(Set.of(RoleName.FRAUD_ANALYST, RoleName.RISK_MANAGER, RoleName.COMPLIANCE_OFFICER),
                NotificationType.CRITICAL_TRANSACTION,
                "Critical fraud alert " + alert.getAlertNumber(),
                "Immediate review required for transaction " + alert.getTransaction().getTransactionReference()
                        + " with risk score " + alert.getRiskScore(), alert.getId());
    }

    @Transactional
    public void notifyCaseAssigned(InvestigationCase value, User assignee) {
        create(assignee, NotificationType.CASE_ASSIGNED, "Case assigned: " + value.getCaseNumber(),
                "Investigation case " + value.getCaseNumber() + " has been assigned to you", value.getId());
    }

    @Transactional
    public void notifyCaseEscalated(InvestigationCase value) {
        notifyRoles(Set.of(RoleName.COMPLIANCE_OFFICER), NotificationType.CASE_ESCALATED,
                "Case escalated: " + value.getCaseNumber(),
                "Investigation case " + value.getCaseNumber() + " requires compliance review", value.getId());
    }

    @Transactional
    public void notifyRuleUpdated(FraudRule rule) {
        notifyRoles(Set.of(RoleName.RISK_MANAGER), NotificationType.RULE_UPDATED,
                "Fraud rule updated: " + rule.getRuleCode(),
                "Rule " + rule.getRuleName() + " was updated", rule.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> listMine(int page, int size) {
        return PageResponse.from(repository.findAllByRecipientUserId(currentUser.id(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))),
                NotificationResponse::from);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId) {
        UUID userId = currentUser.id();
        Notification value = repository.findByIdAndRecipientUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        value.markAsRead();
        return NotificationResponse.from(repository.saveAndFlush(value));
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return repository.countByRecipientUserIdAndReadFlagFalse(currentUser.id());
    }

    private void notifyRoles(Set<RoleName> roles, NotificationType type,
            String title, String message, UUID referenceId) {
        LinkedHashMap<UUID, User> recipients = new LinkedHashMap<>();
        roles.forEach(role -> userRoleRepository.findUsersByRoleAndStatus(role, UserStatus.ACTIVE)
                .forEach(user -> recipients.put(user.getId(), user)));
        recipients.values().forEach(user -> create(user, type, title, message, referenceId));
    }

    private void create(User recipient, NotificationType type,
            String title, String message, UUID referenceId) {
        if (!repository.existsByRecipientUserIdAndReferenceIdAndType(recipient.getId(), referenceId, type)) {
            Notification saved = repository.save(new Notification(recipient, title, message, type, referenceId));
            eventPublisher.publishEvent(new NotificationCreatedEvent(
                    recipient.getEmail(), NotificationResponse.from(saved)));
        }
    }
}
