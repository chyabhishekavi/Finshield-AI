package com.finshield.backend.auth.security;

import com.finshield.backend.user.domain.RoleName;
import com.finshield.backend.user.domain.User;
import com.finshield.backend.user.domain.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class FinshieldUserPrincipal implements UserDetails {

    private final UUID id;
    private final String fullName;
    private final String email;
    private final String passwordHash;
    private final UserStatus status;
    private final Instant lastLoginAt;
    private final List<RoleName> roles;
    private final List<GrantedAuthority> authorities;

    private FinshieldUserPrincipal(User user, Collection<RoleName> roles) {
        this.id = Objects.requireNonNull(user.getId(), "persisted user id must not be null");
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.status = user.getStatus();
        this.lastLoginAt = user.getLastLoginAt();
        this.roles = roles.stream().distinct().sorted().toList();
        this.authorities = this.roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
    }

    public static FinshieldUserPrincipal from(User user, Collection<RoleName> roles) {
        return new FinshieldUserPrincipal(user, roles);
    }

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public List<RoleName> getRoles() {
        return roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
