package com.finshield.backend.notification.domain;

import com.finshield.backend.common.entity.BaseEntity;
import com.finshield.backend.user.domain.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "notifications", uniqueConstraints = @UniqueConstraint(
        name = "uk_notification_recipient_reference_type",
        columnNames = {"recipient_user_id", "reference_id", "type"}), indexes = {
        @Index(name = "idx_notification_recipient_read_created",
                columnList = "recipient_user_id, read_flag, created_at"),
        @Index(name = "idx_notification_type_created", columnList = "type, created_at")
})
public class Notification extends BaseEntity {
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_notification_recipient"))
    private User recipientUser;
    @NotBlank @Size(max = 200) @Column(nullable = false, updatable = false, length = 200)
    private String title;
    @NotBlank @Size(max = 2000) @Column(nullable = false, updatable = false, length = 2000)
    private String message;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 40)
    private NotificationType type;
    @NotNull @Column(name = "reference_id", nullable = false, updatable = false)
    private UUID referenceId;
    @Column(name = "read_flag", nullable = false)
    private boolean readFlag;

    protected Notification() {}
    public Notification(User recipientUser, String title, String message,
            NotificationType type, UUID referenceId) {
        this.recipientUser = Objects.requireNonNull(recipientUser);
        this.title = Objects.requireNonNull(title).trim();
        this.message = Objects.requireNonNull(message).trim();
        this.type = Objects.requireNonNull(type);
        this.referenceId = Objects.requireNonNull(referenceId);
    }
    public void markAsRead() { readFlag = true; }
    public User getRecipientUser() { return recipientUser; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public NotificationType getType() { return type; }
    public UUID getReferenceId() { return referenceId; }
    public boolean isReadFlag() { return readFlag; }
}
