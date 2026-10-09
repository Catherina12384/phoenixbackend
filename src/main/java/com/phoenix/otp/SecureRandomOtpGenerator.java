package com.phoenix.otp;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SecureRandomOtpGenerator implements OtpGenerator {
    private final SecureRandom random = new SecureRandom();
    private final int length;

    public SecureRandomOtpGenerator(OtpProperties props) {
        this.length = props.length();
    }

    @Override
    public String generate() {
        int bound = (int) Math.pow(10, length);
        return String.format("%0" + length + "d", random.nextInt(bound));
    }
}
