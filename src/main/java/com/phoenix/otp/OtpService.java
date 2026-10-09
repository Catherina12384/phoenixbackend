package com.phoenix.otp;

public interface OtpService {
    /** Generates, stores (hashed) and sends a new OTP. Applies rate limits and the resend cooldown. */
    void issue(OtpPurpose purpose, String phone);

    /** Checks a submitted code. Expected outcomes are returned, not thrown, so attempt counts always persist. */
    VerificationResult verify(OtpPurpose purpose, String phone, String otp);
}
