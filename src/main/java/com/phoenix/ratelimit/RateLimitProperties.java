package com.phoenix.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        RateLimit otpPerPhone,
        RateLimit otpPerIp,
        RateLimit otpVerifyPerIp,
        RateLimit loginFailures,
        RateLimit passwordChangeFailures) {}
