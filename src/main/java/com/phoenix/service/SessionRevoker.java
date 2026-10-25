package com.phoenix.service;

import com.phoenix.entity.User;
import com.phoenix.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Signs a user out of every device: bumping tokenVersion invalidates all of their JWTs at once. */
@Component
public class SessionRevoker {
    private final UserRepository users;

    public SessionRevoker(UserRepository users) {
        this.users = users;
    }

    @Transactional
    public void revokeAll(Long userId) {
        users.findById(userId).ifPresent((User u) -> u.setTokenVersion(u.getTokenVersion() + 1));
    }
}
