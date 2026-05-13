package com.finshield.backend.auth.security;

import com.finshield.backend.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentUser {

    public FinshieldUserPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof FinshieldUserPrincipal principal)) {
            throw new UnauthorizedException("Authentication is required");
        }
        return principal;
    }

    public UUID id() {
        return principal().getId();
    }
}
