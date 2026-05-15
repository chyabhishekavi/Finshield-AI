package com.finshield.backend.auth;

import com.finshield.backend.auth.api.AuthResponse;
import com.finshield.backend.auth.api.LoginRequest;
import com.finshield.backend.auth.api.RegisterRequest;
import com.finshield.backend.auth.api.UserResponse;
import com.finshield.backend.auth.security.FinshieldUserDetailsService;
import com.finshield.backend.auth.security.FinshieldUserPrincipal;
import com.finshield.backend.auth.security.JwtService;
import com.finshield.backend.audit.AuditService;
import com.finshield.backend.audit.domain.AuditAction;
import com.finshield.backend.common.exception.BadRequestException;
import com.finshield.backend.common.exception.ResourceNotFoundException;
import com.finshield.backend.user.domain.User;
import com.finshield.backend.user.domain.UserStatus;
import com.finshield.backend.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final FinshieldUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            FinshieldUserDetailsService userDetailsService,
            JwtService jwtService,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BadRequestException("An account with this email already exists");
        }

        User user = new User(
                request.fullName(),
                request.email(),
                passwordEncoder.encode(request.password())
        );
        user.changeStatus(UserStatus.ACTIVE);

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new BadRequestException("An account with this email already exists", exception);
        }

        return issueToken(userDetailsService.loadUserById(user.getId()));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        } catch (AuthenticationException exception) {
            auditService.logIndependent(null, AuditAction.LOGIN, "User", null, null,
                    Map.of("email", request.email().trim().toLowerCase(), "successful", false));
            throw exception;
        }
        FinshieldUserPrincipal authenticatedPrincipal =
                (FinshieldUserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(authenticatedPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", authenticatedPrincipal.getId()));
        Instant previousLogin = user.getLastLoginAt();
        Instant loginTime = Instant.now();
        user.recordSuccessfulLogin(loginTime);
        auditService.logAs(user.getId(), AuditAction.LOGIN, "User", user.getId(),
                previousLogin == null ? null : Map.of("lastLoginAt", previousLogin),
                Map.of("email", user.getEmail(), "lastLoginAt", loginTime, "successful", true));

        return issueToken(userDetailsService.loadUserById(user.getId()));
    }

    private AuthResponse issueToken(FinshieldUserPrincipal principal) {
        return new AuthResponse(
                jwtService.generateAccessToken(principal),
                jwtService.accessTokenTtlSeconds(),
                UserResponse.from(principal)
        );
    }
}
