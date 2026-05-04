package com.finshield.backend.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super("%s not found with identifier: %s".formatted(resourceName, identifier));
    }
}
