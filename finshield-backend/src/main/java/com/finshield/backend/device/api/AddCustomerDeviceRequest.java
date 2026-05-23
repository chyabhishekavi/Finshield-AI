package com.finshield.backend.device.api;

import com.finshield.backend.device.domain.DeviceType;
import com.finshield.backend.device.validation.ValidIpAddress;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddCustomerDeviceRequest(
        @NotBlank @Size(max = 128) String deviceId,
        @NotNull DeviceType deviceType,
        @NotBlank @Size(max = 45) @ValidIpAddress String ipAddress,
        @NotBlank @Size(max = 200) String location,
        Boolean trusted
) {
}
