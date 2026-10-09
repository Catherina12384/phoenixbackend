package com.phoenix.otp;

public interface OtpSender {
    /** @param phoneE164 e.g. +919876543210. Throws {@link com.phoenix.exception.OtpDeliveryException} on failure. */
    void send(String phoneE164, String otp, OtpPurpose purpose);
}
