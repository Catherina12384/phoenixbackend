package com.phoenix.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * The one place that builds the session cookie: httpOnly (JavaScript cannot read it), SameSite=Strict
 * (not sent on cross-site requests), Secure unless app.cookie.secure=false (dev profile only),
 * scoped to /api, and valid as long as the JWT inside it.
 */
@Component
public class AuthCookieService {
    public static final String NAME = "phoenix_session";
    private static final String PATH = "/api";

    private final Duration ttl;
    private final boolean secure;

    public AuthCookieService(@Value("${app.jwt.ttl-minutes}") long ttlMinutes,
                             @Value("${app.cookie.secure:true}") boolean secure) {
        this.ttl = Duration.ofMinutes(ttlMinutes);
        this.secure = secure;
    }

    public ResponseCookie issue(String token) {
        return build(token, ttl);
    }

    public ResponseCookie clear() {
        return build("", Duration.ZERO);
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(PATH)
                .maxAge(maxAge)
                .build();
    }
}
