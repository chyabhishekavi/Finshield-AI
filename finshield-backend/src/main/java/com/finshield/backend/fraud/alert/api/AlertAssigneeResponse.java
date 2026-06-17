package com.finshield.backend.fraud.alert.api;

import com.finshield.backend.user.domain.User;
import java.util.UUID;

public record AlertAssigneeResponse(UUID id, String fullName, String email) {
    public static AlertAssigneeResponse from(User user) {
        return new AlertAssigneeResponse(user.getId(), user.getFullName(), user.getEmail());
    }
}
