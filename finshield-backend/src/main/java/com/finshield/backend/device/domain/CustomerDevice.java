package com.finshield.backend.device.domain;

import com.finshield.backend.common.entity.AuditableEntity;
import com.finshield.backend.customer.domain.Customer;
import com.finshield.backend.device.validation.ValidIpAddress;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "customer_devices",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_customer_devices_customer_device",
                columnNames = {"customer_id", "device_id"}
        ),
        indexes = {
                @Index(name = "idx_customer_devices_customer", columnList = "customer_id"),
                @Index(name = "idx_customer_devices_last_seen", columnList = "last_seen_at"),
                @Index(name = "idx_customer_devices_ip", columnList = "ip_address")
        }
)
public class CustomerDevice extends AuditableEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_customer_devices_customer")
    )
    private Customer customer;

    @NotBlank
    @Size(max = 128)
    @Column(name = "device_id", nullable = false, updatable = false, length = 128)
    private String deviceId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 30)
    private DeviceType deviceType;

    @NotBlank
    @ValidIpAddress
    @Size(max = 45)
    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String location;

    @Column(nullable = false)
    private boolean trusted;

    @NotNull
    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private Instant firstSeenAt;

    @NotNull
    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    protected CustomerDevice() {
        // Required by JPA.
    }

    public CustomerDevice(
            Customer customer,
            String deviceId,
            DeviceType deviceType,
            String ipAddress,
            String location,
            boolean trusted,
            Instant firstSeenAt
    ) {
        this.customer = Objects.requireNonNull(customer, "customer must not be null");
        this.deviceId = requireTrimmed(deviceId, "deviceId");
        this.deviceType = Objects.requireNonNull(deviceType, "deviceType must not be null");
        this.ipAddress = requireTrimmed(ipAddress, "ipAddress");
        this.location = requireTrimmed(location, "location");
        this.trusted = trusted;
        this.firstSeenAt = Objects.requireNonNull(firstSeenAt, "firstSeenAt must not be null");
        this.lastSeenAt = firstSeenAt;
    }

    public void updateObservation(
            DeviceType deviceType,
            String ipAddress,
            String location,
            boolean trusted,
            Instant observedAt
    ) {
        this.deviceType = Objects.requireNonNull(deviceType, "deviceType must not be null");
        this.ipAddress = requireTrimmed(ipAddress, "ipAddress");
        this.location = requireTrimmed(location, "location");
        this.trusted = trusted;
        this.lastSeenAt = Objects.requireNonNull(observedAt, "observedAt must not be null");
    }

    private static String requireTrimmed(String value, String field) {
        return Objects.requireNonNull(value, field + " must not be null").trim();
    }

    public Customer getCustomer() { return customer; }
    public String getDeviceId() { return deviceId; }
    public DeviceType getDeviceType() { return deviceType; }
    public String getIpAddress() { return ipAddress; }
    public String getLocation() { return location; }
    public boolean isTrusted() { return trusted; }
    public Instant getFirstSeenAt() { return firstSeenAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
}
