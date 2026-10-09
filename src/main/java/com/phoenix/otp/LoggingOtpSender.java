package com.phoenix.otp;

import com.phoenix.common.Masks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/** DEV ONLY: prints the OTP instead of sending an SMS. Refuses to start outside the dev profile. */
@Component
@ConditionalOnProperty(name = "app.otp.sender", havingValue = "logging")
public class LoggingOtpSender implements OtpSender {
    private static final Logger log = LoggerFactory.getLogger(LoggingOtpSender.class);

    public LoggingOtpSender(Environment env) {
        if (!env.acceptsProfiles(Profiles.of("dev"))) {
            throw new IllegalStateException(
                    "OTP_SENDER=logging prints OTPs in the log and is only allowed with the 'dev' profile");
        }
    }

    @Override
    public void send(String phoneE164, String otp, OtpPurpose purpose) {
        log.warn("DEV ONLY - {} OTP for {} is {}", purpose, Masks.phone(phoneE164), otp);
    }
}
