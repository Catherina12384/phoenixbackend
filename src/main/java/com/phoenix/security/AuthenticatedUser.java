package com.phoenix.security;

import com.phoenix.entity.User;

import java.security.Principal;

/** The authenticated principal placed in the SecurityContext by {@link JwtAuthFilter}. */
public record AuthenticatedUser(Long id, String email, User.Role role) implements Principal {
    @Override
    public String getName() {
        return email;
    }
}
