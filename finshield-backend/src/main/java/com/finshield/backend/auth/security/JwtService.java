package com.finshield.backend.auth.security;

import com.finshield.backend.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(FinshieldUserPrincipal principal) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        List<String> roles = principal.getRoles().stream().map(Enum::name).toList();

        return Jwts.builder()
                .subject(principal.getId().toString())
                .claim("email", principal.getEmail())
                .claim("roles", roles)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public boolean isTokenValid(String token, FinshieldUserPrincipal principal) {
        try {
            Claims claims = parseClaims(token);
            return principal.getId().toString().equals(claims.getSubject())
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public long accessTokenTtlSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
