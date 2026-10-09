package com.phoenix.security;

import com.phoenix.audit.AuditAction;
import com.phoenix.audit.AuditLogger;
import com.phoenix.config.SecurityProperties;
import com.phoenix.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    /*
     * Access matrix
     *                                   anonymous  CUSTOMER  STAFF  ADMIN
     *  GET  /api/products, /api/dealers     yes       yes      yes    yes
     *  PUT  /api/products/{id}              no        no       yes    yes
     *  /api/admin/** (create/delete products, dealers CRUD, users, audit log)  ADMIN only
     */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtService jwt, UserRepository users,
                                    AuditLogger audit, CorsConfigurationSource cors) throws Exception {
        http
            .csrf(c -> c.disable())   // stateless API, token in Authorization header
            .cors(c -> c.configurationSource(cors))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((req, res, ex) -> {
                    log.warn("Access denied: {} {}", req.getMethod(), req.getRequestURI());
                    audit.log(AuditAction.ACCESS_DENIED, "endpoint", req.getMethod() + " " + req.getRequestURI(), false, null);
                    res.setStatus(HttpStatus.FORBIDDEN.value());
                    res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                    res.getWriter().write("{\"status\":403,\"title\":\"Forbidden\",\"detail\":\"You do not have permission to do that.\"}");
                }))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/error").permitAll()   // otherwise 404/409/400 bodies turn into 401
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll() // only exist when SWAGGER_ENABLED=true
                .requestMatchers(HttpMethod.GET, "/api/dealers/**", "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.POST,
                        "/api/auth/register/request-otp", "/api/auth/register/verify",
                        "/api/auth/login",
                        "/api/auth/forgot-password/request-otp", "/api/auth/forgot-password/reset").permitAll()
                .requestMatchers(HttpMethod.PUT, "/api/products/*").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .addFilterBefore(new JwtAuthFilter(jwt, users), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(SecurityProperties props) {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(props.corsAllowedOrigins());
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
        c.setExposedHeaders(List.of("X-Request-Id", "Retry-After"));
        c.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", c);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
