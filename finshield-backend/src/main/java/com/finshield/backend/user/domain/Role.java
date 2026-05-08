package com.finshield.backend.user.domain;

import com.finshield.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Objects;

@Entity
@Table(name = "roles")
public class Role extends BaseEntity {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, updatable = false, length = 40)
    private RoleName name;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String description;

    protected Role() {
        // Required by JPA.
    }

    public Role(RoleName name, String description) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null").trim();
    }

    public RoleName getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void updateDescription(String description) {
        this.description = Objects.requireNonNull(description, "description must not be null").trim();
    }
}
