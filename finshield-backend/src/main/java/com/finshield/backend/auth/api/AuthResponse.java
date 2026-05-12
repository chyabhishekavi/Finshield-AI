package com.finshield.backend.auth.api;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user
) {

    public AuthResponse(String accessToken, long expiresInSeconds, UserResponse user) {
        this(accessToken, "Bearer", expiresInSeconds, user);
    }
}
