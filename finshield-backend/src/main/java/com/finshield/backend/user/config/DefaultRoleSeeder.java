package com.finshield.backend.user.config;

import com.finshield.backend.user.domain.Role;
import com.finshield.backend.user.domain.RoleName;
import com.finshield.backend.user.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@Order(0)
public class DefaultRoleSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultRoleSeeder.class);

    private static final Map<RoleName, String> DEFAULT_ROLES = defaultRoles();

    private final RoleRepository roleRepository;

    public DefaultRoleSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Role> missingRoles = DEFAULT_ROLES.entrySet().stream()
                .filter(entry -> !roleRepository.existsByName(entry.getKey()))
                .map(entry -> new Role(entry.getKey(), entry.getValue()))
                .toList();

        if (!missingRoles.isEmpty()) {
            roleRepository.saveAll(missingRoles);
            log.info("Seeded {} default RBAC roles", missingRoles.size());
        }
    }

    private static Map<RoleName, String> defaultRoles() {
        Map<RoleName, String> roles = new EnumMap<>(RoleName.class);
        roles.put(RoleName.ADMIN, "Manages users, roles, platform configuration, and operational settings");
        roles.put(RoleName.FRAUD_ANALYST, "Triages fraud alerts and records investigation outcomes");
        roles.put(RoleName.AML_INVESTIGATOR, "Investigates AML alerts, customer behavior, and linked activity");
        roles.put(RoleName.COMPLIANCE_OFFICER, "Reviews compliance decisions and approves regulatory reporting");
        roles.put(RoleName.RISK_MANAGER, "Manages detection policy, risk thresholds, and control oversight");
        return Map.copyOf(roles);
    }
}
