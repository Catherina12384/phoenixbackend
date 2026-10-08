package com.phoenix.service;

import com.phoenix.dto.AuthResponse;
import com.phoenix.dto.LoginRequest;
import com.phoenix.dto.RegisterRequest;
import com.phoenix.dto.UserDto;
import com.phoenix.entity.User;
import com.phoenix.repository.UserRepository;
import com.phoenix.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DefaultAuthService implements AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public DefaultAuthService(UserRepository users,
                              PasswordEncoder passwordEncoder,
                              JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPhone(normalizeOptional(request.phone()));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(User.Role.CUSTOMER);

        users.save(user);
        return new AuthResponse(jwtService.issue(user), UserDto.from(user));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        User user = users.findByEmail(email)
                .filter(candidate -> passwordEncoder.matches(
                        request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        return new AuthResponse(jwtService.issue(user), UserDto.from(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto me(String email) {
        return users.findByEmail(normalizeEmail(email))
                .map(UserDto::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user no longer exists"));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
