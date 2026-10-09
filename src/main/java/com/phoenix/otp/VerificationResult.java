package com.phoenix.otp;

public enum VerificationResult {
    VERIFIED,
    /** Wrong code; attempts remain. */
    INVALID,
    /** No live OTP for this phone (never sent, expired, already used, or destroyed after too many attempts). */
    EXPIRED,
    /** This wrong code used the last attempt: the OTP is destroyed and the phone is locked out. */
    LOCKED_OUT
}
