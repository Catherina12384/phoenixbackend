package com.phoenix.otp;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * HMAC-SHA256 keyed with OTP_HMAC_SECRET over purpose|phone|otp. A plain SHA-256 of a 6-digit code
 * could be brute-forced from a database leak in microseconds; the secret key makes a leak useless.
 */
@Component
public class HmacSha256OtpHasher implements OtpHasher {
    private static final String ALGORITHM = "HmacSHA256";
    private final byte[] key;

    public HmacSha256OtpHasher(OtpProperties props) {
        String secret = props.hmacSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("OTP_HMAC_SECRET must be at least 32 characters");
        }
        this.key = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String hash(OtpPurpose purpose, String phone, String otp) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);      // Mac is not thread-safe: one per call
            mac.init(new SecretKeySpec(key, ALGORITHM));
            byte[] digest = mac.doFinal((purpose.name() + "|" + phone + "|" + otp).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }

    @Override
    public boolean matches(OtpPurpose purpose, String phone, String otp, String expectedHash) {
        byte[] actual = hash(purpose, phone, otp).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(actual, expectedHash.getBytes(StandardCharsets.UTF_8));   // constant time
    }
}
