package com.finshield.backend.device.api;

import com.finshield.backend.device.domain.CustomerDevice;
import com.finshield.backend.device.domain.DeviceType;

import java.time.Instant;
import java.util.UUID;

public record CustomerDeviceResponse(
        UUID id,
        UUID customerId,
        String deviceId,
        DeviceType deviceType,
        String ipAddress,
        String location,
        boolean trusted,
        Instant firstSeenAt,
        Instant lastSeenAt,
        Instant updatedAt
) {

    public static CustomerDeviceResponse from(CustomerDevice device) {
        return new CustomerDeviceResponse(
                device.getId(),
                device.getCustomer().getId(),
                device.getDeviceId(),
                device.getDeviceType(),
                device.getIpAddress(),
                device.getLocation(),
                device.isTrusted(),
                device.getFirstSeenAt(),
                device.getLastSeenAt(),
                device.getUpdatedAt()
        );
    }
}
