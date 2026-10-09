package com.phoenix.otp;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.otp")
public record OtpProperties(
        @DefaultValue("6") @Min(4) @Max(8) int length,
        @DefaultValue("5m") Duration ttl,
        @DefaultValue("3") @Min(1) int maxAttempts,
        @DefaultValue("60s") Duration resendCooldown,
        @DefaultValue("15m") Duration lockout,
        @NotBlank @Size(min = 32, message = "OTP_HMAC_SECRET must be at least 32 characters") String hmacSecret,
        @DefaultValue("msg91") String sender,
        @DefaultValue Msg91 msg91) {

    public record Msg91(
            @DefaultValue("https://control.msg91.com/api/v5/otp") String url,
            String authKey,
            String templateId,
            @DefaultValue("3s") Duration timeout) {
        @Override
        public String toString() { return "Msg91[redacted]"; }
    }

    /** Never print secrets, even if someone logs the properties object. */
    @Override
    public String toString() { return "OtpProperties[redacted]"; }
}
