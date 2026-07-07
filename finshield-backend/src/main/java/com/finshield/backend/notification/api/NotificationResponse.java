package com.finshield.backend.notification.api;

import com.finshield.backend.notification.domain.Notification;
import com.finshield.backend.notification.domain.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(UUID id, UUID recipientUserId, String title,
        String message, NotificationType type, boolean readFlag,
        UUID referenceId, Instant createdAt) {
    public static NotificationResponse from(Notification value) {
        return new NotificationResponse(value.getId(), value.getRecipientUser().getId(),
                value.getTitle(), value.getMessage(), value.getType(), value.isReadFlag(),
                value.getReferenceId(), value.getCreatedAt());
    }
}
