package com.phoenix.otp;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class HmacSha256OtpHasherTest {
    static OtpProperties props(String secret) {
        return new OtpProperties(6, Duration.ofMinutes(5), 3, Duration.ofSeconds(60), Duration.ofMinutes(15),
                secret, "logging", new OtpProperties.Msg91(null, null, null, Duration.ofSeconds(3)));
    }

    private final HmacSha256OtpHasher hasher = new HmacSha256OtpHasher(props("x".repeat(32)));

    @Test void matchesCorrectCode() {
        String h = hasher.hash(OtpPurpose.REGISTER, "+919500288164", "123456");
        assertTrue(hasher.matches(OtpPurpose.REGISTER, "+919500288164", "123456", h));
    }

    @Test void rejectsWrongCode() {
        String h = hasher.hash(OtpPurpose.REGISTER, "+919500288164", "123456");
        assertFalse(hasher.matches(OtpPurpose.REGISTER, "+919500288164", "654321", h));
    }

    @Test void hashIsBoundToPhoneAndPurpose() {
        String h = hasher.hash(OtpPurpose.REGISTER, "+919500288164", "123456");
        assertFalse(hasher.matches(OtpPurpose.REGISTER, "+919842125620", "123456", h));
        assertFalse(hasher.matches(OtpPurpose.RESET_PASSWORD, "+919500288164", "123456", h));
    }

    @Test void hashDependsOnSecret() {
        var other = new HmacSha256OtpHasher(props("y".repeat(32)));
        assertNotEquals(hasher.hash(OtpPurpose.REGISTER, "+919500288164", "123456"),
                other.hash(OtpPurpose.REGISTER, "+919500288164", "123456"));
    }

    @Test void hashDoesNotContainPlainCode() {
        assertFalse(hasher.hash(OtpPurpose.REGISTER, "+919500288164", "123456").contains("123456"));
    }

    @Test void shortSecretRefusesToStart() {
        assertThrows(IllegalStateException.class, () -> new HmacSha256OtpHasher(props("short")));
    }
}
