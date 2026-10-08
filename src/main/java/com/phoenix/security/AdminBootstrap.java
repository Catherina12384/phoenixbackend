package com.phoenix.security;

import com.phoenix.entity.User;
import com.phoenix.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first admin from ADMIN_EMAIL / ADMIN_PASSWORD if that account does not exist yet. */
@Component
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String name, email, password;

    public AdminBootstrap(UserRepository users, PasswordEncoder encoder,
                          @Value("${app.admin.name}") String name,
                          @Value("${app.admin.email}") String email,
                          @Value("${app.admin.password}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) return;
        String e = email.trim().toLowerCase();
        if (users.existsByEmail(e)) return;
        User admin = new User();
        admin.setName(name);
        admin.setEmail(e);
        admin.setPasswordHash(encoder.encode(password));
        admin.setRole(User.Role.ADMIN);
        users.save(admin);
        log.info("Created admin account {}", e);
    }
}