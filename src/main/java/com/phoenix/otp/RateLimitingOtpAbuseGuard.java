package com.phoenix.otp;

import com.phoenix.common.ClientContext;
import com.phoenix.common.Masks;
import com.phoenix.ratelimit.RateLimit;
import com.phoenix.ratelimit.RateLimitProperties;
import com.phoenix.ratelimit.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RateLimitingOtpAbuseGuard implements OtpAbuseGuard {
    private static final Logger log = LoggerFactory.getLogger(RateLimitingOtpAbuseGuard.class);

    private final RateLimiter limiter;
    private final RateLimitProperties rates;
    private final ClientContext client;
    private final RateLimit lockoutRule;

    public RateLimitingOtpAbuseGuard(RateLimiter limiter, RateLimitProperties rates,
                                     ClientContext client, OtpProperties otp) {
        this.limiter = limiter;
        this.rates = rates;
        this.client = client;
        this.lockoutRule = new RateLimit(1, otp.lockout());
    }

    @Override
    public void beforeIssue(OtpPurpose purpose, String phone) {
        limiter.assertNotReached(lockKey(phone), lockoutRule);
        limiter.consume("otp-ip:" + client.ip(), rates.otpPerIp());
        limiter.consume("otp-phone:" + phone, rates.otpPerPhone());
    }

    @Override
    public void beforeVerify() {
        limiter.consume("otp-verify-ip:" + client.ip(), rates.otpVerifyPerIp());
    }

    @Override
    public void onLockout(OtpPurpose purpose, String phone) {
        log.warn("OTP lockout for {} ({}) after too many wrong codes", Masks.phone(phone), purpose);
        limiter.record(lockKey(phone), lockoutRule);
    }

    private static String lockKey(String phone) {
        return "otp-lock:" + phone;
    }
}
