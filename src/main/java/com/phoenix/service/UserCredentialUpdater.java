package com.phoenix.service;

import com.phoenix.entity.User;
import com.phoenix.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** The one place that changes a stored password. Always revokes existing tokens. */
@Component
public class UserCredentialUpdater {
    private final UserRepository users;

    public UserCredentialUpdater(UserRepository users) {
        this.users = users;
    }

    @Transactional
    public void setPassword(Long userId, String encodedHash, boolean mustChangePassword) {
        User u = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        u.setPasswordHash(encodedHash);
        u.setMustChangePassword(mustChangePassword);
        u.setTokenVersion(u.getTokenVersion() + 1);
    }
}
