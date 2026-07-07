package com.finshield.backend.notification.repository;

import com.finshield.backend.notification.domain.Notification;
import com.finshield.backend.notification.domain.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    boolean existsByRecipientUserIdAndReferenceIdAndType(
            UUID recipientUserId, UUID referenceId, NotificationType type);
    Page<Notification> findAllByRecipientUserId(UUID recipientUserId, Pageable pageable);
    Optional<Notification> findByIdAndRecipientUserId(UUID id, UUID recipientUserId);
    long countByRecipientUserIdAndReadFlagFalse(UUID recipientUserId);
}
