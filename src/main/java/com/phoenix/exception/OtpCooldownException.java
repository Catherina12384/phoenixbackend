package com.phoenix.exception;

/** Thrown when an OTP is requested again before the resend cooldown has passed. */
public class OtpCooldownException extends RateLimitExceededException {
    public OtpCooldownException(long retryAfterSeconds) {
        super("Please wait before requesting another OTP.", retryAfterSeconds);
    }
}
