package com.phoenix.security;

import com.phoenix.common.Masks;
import com.phoenix.common.PhoneNumbers;
import com.phoenix.entity.User;
import com.phoenix.repository.UserRepository;
import com.phoenix.validation.PasswordPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first admin from ADMIN_EMAIL / ADMIN_PHONE / ADMIN_PASSWORD if that account does not exist yet. */
@Component
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String name, email, phone, password;

    public AdminBootstrap(UserRepository users, PasswordEncoder encoder,
                          @Value("${app.admin.name}") String name,
                          @Value("${app.admin.email}") String email,
                          @Value("${app.admin.phone}") String phone,
                          @Value("${app.admin.password}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() && phone.isBlank() && password.isBlank()) {
            log.info("Admin bootstrap skipped: ADMIN_EMAIL / ADMIN_PHONE / ADMIN_PASSWORD not set");
            return;
        }
        if (email.isBlank() || phone.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Set all of ADMIN_EMAIL, ADMIN_PHONE and ADMIN_PASSWORD, or none");
        }
        if (!PasswordPolicy.isValid(password)) {
            throw new IllegalStateException("ADMIN_PASSWORD does not meet the password policy");
        }
        String e = email.trim().toLowerCase();
        String p = PhoneNumbers.normalize(phone);
        if (users.existsByEmail(e)) {
            log.info("Admin bootstrap skipped: {} already exists", e);
            return;
        }
        if (users.existsByPhone(p)) {
            throw new IllegalStateException("ADMIN_PHONE " + Masks.phone(p) + " already belongs to another account");
        }
        User admin = new User();
        admin.setName(name);
        admin.setEmail(e);
        admin.setPhone(p);
        admin.setPasswordHash(encoder.encode(password));
        admin.setRole(User.Role.ADMIN);
        users.save(admin);
        log.info("Created admin account {}", e);
    }
}
