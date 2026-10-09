package com.phoenix.otp;

public interface OtpHasher {
    String hash(OtpPurpose purpose, String phone, String otp);

    boolean matches(OtpPurpose purpose, String phone, String otp, String expectedHash);
}
