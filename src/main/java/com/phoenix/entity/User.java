package com.phoenix.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class User {
    public enum Role { CUSTOMER, STAFF, ADMIN }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    /** E.164, always +91XXXXXXXXXX. Mandatory and unique. */
    @Column(nullable = false, unique = true)
    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CUSTOMER;

    /** Soft delete: false means the account can no longer log in or use existing tokens. */
    @Column(nullable = false)
    private boolean active = true;

    /** True for staff created with an admin-set temporary password. */
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    /** Embedded in every JWT; incrementing it instantly invalidates all tokens of this user. */
    @Column(name = "token_version", nullable = false)
    private int tokenVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
