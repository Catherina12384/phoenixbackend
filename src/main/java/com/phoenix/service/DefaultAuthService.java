package com.phoenix.service;

import com.phoenix.audit.AuditAction;
import com.phoenix.audit.AuditLogger;
import com.phoenix.common.ClientContext;
import com.phoenix.dto.AuthResponse;
import com.phoenix.dto.ChangePasswordRequest;
import com.phoenix.dto.LoginRequest;
import com.phoenix.dto.UserDto;
import com.phoenix.entity.User;
import com.phoenix.ratelimit.RateLimitProperties;
import com.phoenix.ratelimit.RateLimiter;
import com.phoenix.repository.UserRepository;
import com.phoenix.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

@Service
public class DefaultAuthService implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(DefaultAuthService.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final RateLimiter limiter;
    private final RateLimitProperties rates;
    private final ClientContext client;
    private final AuditLogger audit;
    private final UserCredentialUpdater credentials;
    /** Compared against when the email is unknown, so response time does not reveal which emails exist. */
    private final String dummyHash;

    public DefaultAuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, RateLimiter limiter,
                              RateLimitProperties rates, ClientContext client, AuditLogger audit,
                              UserCredentialUpdater credentials) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.limiter = limiter;
        this.rates = rates;
        this.client = client;
        this.audit = audit;
        this.credentials = credentials;
        this.dummyHash = encoder.encode("not-a-real-password");
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        String key = "login:" + client.ip() + ":" + (email.length() > 254 ? email.substring(0, 254) : email);

        limiter.assertNotReached(key, rates.loginFailures());      // 429 while locked out

        Optional<User> found = users.findByEmail(email);
        boolean passwordOk = encoder.matches(request.password(), found.map(User::getPasswordHash).orElse(dummyHash));

        if (found.isEmpty() || !passwordOk || !found.get().isActive()) {
            limiter.record(key, rates.loginFailures());
            audit.logAs(email, null, AuditAction.LOGIN_FAILED, "user", null, false, null);
            log.warn("Login failed for {}", email);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        User user = found.get();
        limiter.reset(key);
        audit.logAs(user.getEmail(), user.getRole().name(), AuditAction.LOGIN_SUCCESS, "user", String.valueOf(user.getId()),
                true, user.isMustChangePassword() ? Map.of("mustChangePassword", true) : null);
        log.info("Login succeeded for {} ({})", user.getEmail(), user.getRole());
        return new AuthResponse(jwt.issue(user), UserDto.from(user));
    }

    @Override
    public UserDto me(String email) {
        return users.findByEmail(email.trim().toLowerCase())
                .map(UserDto::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user no longer exists"));
    }

    @Override
    public AuthResponse changePassword(String email, ChangePasswordRequest request) {
        User user = users.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user no longer exists"));
        String key = "pwchange:" + user.getId();
        String target = String.valueOf(user.getId());

        limiter.assertNotReached(key, rates.passwordChangeFailures());
        if (!encoder.matches(request.currentPassword(), user.getPasswordHash())) {
            limiter.record(key, rates.passwordChangeFailures());
            audit.log(AuditAction.PASSWORD_CHANGE_FAILED, "user", target, false, null);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        if (request.newPassword().equals(request.currentPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be different from the current one");
        }

        credentials.setPassword(user.getId(), encoder.encode(request.newPassword()), false);
        limiter.reset(key);
        audit.log(AuditAction.PASSWORD_CHANGED, "user", target, true, null);
        log.info("Password changed for {}", user.getEmail());

        User fresh = users.findById(user.getId()).orElseThrow();
        return new AuthResponse(jwt.issue(fresh), UserDto.from(fresh));
    }
}
