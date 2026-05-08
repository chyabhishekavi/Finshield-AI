package com.finshield.backend.user.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.finshield.backend.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(
        name = "app_users",
        indexes = {
                @Index(name = "idx_app_users_status", columnList = "status"),
                @Index(name = "idx_app_users_last_login_at", columnList = "last_login_at")
        }
)
public class User extends AuditableEntity {

    @NotBlank
    @Size(min = 2, max = 150)
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @NotBlank
    @Email
    @Size(max = 254)
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @JsonIgnore
    @NotBlank
    @Size(min = 60, max = 255)
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status = UserStatus.PENDING_ACTIVATION;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected User() {
        // Required by JPA.
    }

    public User(String fullName, String email, String passwordHash) {
        this.fullName = normalizeName(fullName);
        this.email = normalizeEmail(email);
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void updateProfile(String fullName, String email) {
        this.fullName = normalizeName(fullName);
        this.email = normalizeEmail(email);
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
    }

    public void changeStatus(UserStatus status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public void recordSuccessfulLogin(Instant loginTime) {
        this.lastLoginAt = Objects.requireNonNull(loginTime, "loginTime must not be null");
    }

    @PrePersist
    @PreUpdate
    void normalizeFields() {
        fullName = normalizeName(fullName);
        email = normalizeEmail(email);
    }

    private static String normalizeName(String value) {
        return Objects.requireNonNull(value, "fullName must not be null").trim();
    }

    private static String normalizeEmail(String value) {
        return Objects.requireNonNull(value, "email must not be null").trim().toLowerCase(Locale.ROOT);
    }
}
