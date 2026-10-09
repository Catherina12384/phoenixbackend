package com.phoenix.otp;

/** Abuse policy around OTPs (rate limits, lockout), kept apart from the OTP mechanics. */
public interface OtpAbuseGuard {
    /** Throws if the phone is locked out or the phone/IP request limits are exceeded. */
    void beforeIssue(OtpPurpose purpose, String phone);

    /** Throws if the caller's IP has made too many verification attempts. */
    void beforeVerify();

    /** Blocks the phone from requesting new OTPs for the configured lockout period. */
    void onLockout(OtpPurpose purpose, String phone);
}
