package com.phoenix.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(List<String> trustedProxies, List<String> corsAllowedOrigins) {
    public SecurityProperties {
        trustedProxies = trustedProxies == null ? List.of() : trustedProxies.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        corsAllowedOrigins = corsAllowedOrigins == null ? List.of() : corsAllowedOrigins.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
