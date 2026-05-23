package com.finshield.backend.device.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.UnknownHostException;

public class IpAddressValidator implements ConstraintValidator<ValidIpAddress, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (value.isBlank()) {
            return false;
        }

        if (!value.contains(":")) {
            return isValidIpv4(value);
        }

        if (!value.matches("[0-9A-Fa-f:]+")) {
            return false;
        }
        try {
            return InetAddress.getByName(value) instanceof Inet6Address;
        } catch (UnknownHostException exception) {
            return false;
        }
    }

    private boolean isValidIpv4(String value) {
        String[] octets = value.split("\\.", -1);
        if (octets.length != 4) {
            return false;
        }
        for (String octet : octets) {
            if (!octet.matches("[0-9]{1,3}")) {
                return false;
            }
            int numericValue = Integer.parseInt(octet);
            if (numericValue > 255) {
                return false;
            }
        }
        return true;
    }
}
