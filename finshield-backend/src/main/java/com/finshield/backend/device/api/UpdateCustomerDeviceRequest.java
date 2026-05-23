package com.finshield.backend.device.api;

import com.finshield.backend.device.domain.DeviceType;
import com.finshield.backend.device.validation.ValidIpAddress;
import jakarta.validation.constraints.Size;

public record UpdateCustomerDeviceRequest(
        DeviceType deviceType,
        @Size(max = 45) @ValidIpAddress String ipAddress,
        @Size(min = 1, max = 200) String location,
        Boolean trusted
) {
}
