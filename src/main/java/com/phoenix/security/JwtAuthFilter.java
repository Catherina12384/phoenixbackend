package com.phoenix.security;

import com.phoenix.entity.User;
import com.phoenix.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Not a @Component on purpose: SecurityConfig adds it to the security chain only.
 * The token is read from the httpOnly session cookie (the browser) or, as a fallback for Swagger and
 * scripts, from an "Authorization: Bearer" header. It is accepted only if the user still exists, is active
 * and the token version matches, so deactivation, password changes, resets and logout take effect immediately.
 * A user who must change a temporary password gets a single authority that opens nothing but
 * /api/auth/change-password, /api/auth/me and /api/auth/logout (see SecurityConfig).
 */
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwt;
    private final UserRepository users;

    public JwtAuthFilter(JwtService jwt, UserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        resolveToken(req).ifPresent(token -> jwt.parse(token).ifPresentOrElse(this::authenticate,
                () -> log.debug("Rejected invalid or expired token")));
        chain.doFilter(req, res);
    }

    private static Optional<String> resolveToken(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (AuthCookieService.NAME.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                    return Optional.of(c.getValue());
                }
            }
        }
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return Optional.of(header.substring(7));
        }
        return Optional.empty();
    }

    private void authenticate(Claims claims) {
        Integer tv = claims.get("tv", Integer.class);
        users.findByEmail(claims.getSubject())
                .filter(User::isActive)
                .filter(u -> tv != null && tv == u.getTokenVersion())
                .ifPresentOrElse(u -> {
                    String authority = u.isMustChangePassword() ? "ROLE_PASSWORD_CHANGE_REQUIRED" : "ROLE_" + u.getRole().name();
                    var auth = new UsernamePasswordAuthenticationToken(
                            new AuthenticatedUser(u.getId(), u.getEmail(), u.getRole()), null,
                            List.of(new SimpleGrantedAuthority(authority)));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    MDC.put("user", u.getEmail());
                }, () -> log.info("Rejected token for {}: user inactive, missing or token revoked", claims.getSubject()));
    }
}