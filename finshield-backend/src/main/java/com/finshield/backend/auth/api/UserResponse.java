package com.finshield.backend.auth.api;

import com.finshield.backend.auth.security.FinshieldUserPrincipal;
import com.finshield.backend.user.domain.RoleName;
import com.finshield.backend.user.domain.UserStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String email,
        UserStatus status,
        Instant lastLoginAt,
        List<RoleName> roles
) {

    public static UserResponse from(FinshieldUserPrincipal principal) {
        return new UserResponse(
                principal.getId(),
                principal.getFullName(),
                principal.getEmail(),
                principal.getStatus(),
                principal.getLastLoginAt(),
                List.copyOf(principal.getRoles())
        );
    }
}
