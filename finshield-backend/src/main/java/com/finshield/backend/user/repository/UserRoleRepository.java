package com.finshield.backend.user.repository;

import com.finshield.backend.user.domain.RoleName;
import com.finshield.backend.user.domain.UserRole;
import com.finshield.backend.user.domain.User;
import com.finshield.backend.user.domain.UserStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    @EntityGraph(attributePaths = "role")
    List<UserRole> findAllByUserId(UUID userId);

    boolean existsByUserIdAndRoleName(UUID userId, RoleName roleName);

    @Query("""
            select distinct ur.user from UserRole ur
            where ur.role.name = :roleName and ur.user.status = :status
            """)
    List<User> findUsersByRoleAndStatus(
            @Param("roleName") RoleName roleName,
            @Param("status") UserStatus status
    );

    void deleteByUserIdAndRoleId(UUID userId, UUID roleId);
}
