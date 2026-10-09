package com.phoenix.service;

import com.phoenix.entity.PendingRegistration;
import com.phoenix.entity.User;
import com.phoenix.repository.PendingRegistrationRepository;
import com.phoenix.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

/** Short database transactions for signup, kept apart from the OTP flow so OTP attempt counts never roll back. */
@Component
public class RegistrationPersistence {
    private final PendingRegistrationRepository pending;
    private final UserRepository users;

    public RegistrationPersistence(PendingRegistrationRepository pending, UserRepository users) {
        this.pending = pending;
        this.users = users;
    }

    @Transactional
    public PendingRegistration savePending(String name, String email, String phone, String passwordHash, Instant expiresAt) {
        pending.deleteByPhoneOrEmail(phone, email);     // a newer attempt replaces older ones
        return pending.save(new PendingRegistration(name, email, phone, passwordHash, expiresAt));
    }

    /** Creates the CUSTOMER account from the pending signup and removes the pending row, atomically. */
    @Transactional
    public User complete(UUID pendingId) {
        PendingRegistration p = pending.findById(pendingId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration expired. Please start again."));
        User user = new User();
        user.setName(p.getName());
        user.setEmail(p.getEmail());
        user.setPhone(p.getPhone());
        user.setPasswordHash(p.getPasswordHash());
        user.setRole(User.Role.CUSTOMER);
        User saved = users.saveAndFlush(user);          // unique violations surface here as 409
        pending.delete(p);
        return saved;
    }
}
