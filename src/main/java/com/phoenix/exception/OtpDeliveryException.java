package com.phoenix.exception;

/**
 * The SMS provider failed or timed out. Deliberately has NO cause: HTTP client exceptions can
 * embed the request URL, which contains the OTP.
 */
public class OtpDeliveryException extends RuntimeException {
    public OtpDeliveryException(String message) {
        super(message);
    }
}
