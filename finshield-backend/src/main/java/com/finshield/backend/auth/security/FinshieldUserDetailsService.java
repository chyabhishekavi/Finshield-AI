package com.finshield.backend.auth.security;

import com.finshield.backend.user.domain.RoleName;
import com.finshield.backend.user.domain.User;
import com.finshield.backend.user.domain.UserRole;
import com.finshield.backend.user.repository.UserRepository;
import com.finshield.backend.user.repository.UserRoleRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FinshieldUserDetailsService implements UserDetailsService {

    private static final String USER_NOT_FOUND = "Invalid email or password";

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public FinshieldUserDetailsService(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository
    ) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND));
        return toPrincipal(user);
    }

    @Transactional(readOnly = true)
    public FinshieldUserPrincipal loadUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND));
        return toPrincipal(user);
    }

    private FinshieldUserPrincipal toPrincipal(User user) {
        List<RoleName> roles = userRoleRepository.findAllByUserId(user.getId()).stream()
                .map(UserRole::getRole)
                .map(role -> role.getName())
                .toList();
        return FinshieldUserPrincipal.from(user, roles);
    }
}
