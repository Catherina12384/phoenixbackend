package com.phoenix.common;

import com.phoenix.exception.InvalidPhoneException;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The single place that defines what a valid phone number is (India only, stored as +91XXXXXXXXXX). */
public final class PhoneNumbers {
    private static final Pattern INDIA = Pattern.compile("^(?:\\+91|91|0)?([6-9]\\d{9})$");

    private PhoneNumbers() {}

    public static Optional<String> tryNormalize(String raw) {
        if (raw == null) return Optional.empty();
        Matcher m = INDIA.matcher(raw.replaceAll("[\\s\\-()]", ""));
        return m.matches() ? Optional.of("+91" + m.group(1)) : Optional.empty();
    }

    public static String normalize(String raw) {
        return tryNormalize(raw).orElseThrow(InvalidPhoneException::new);
    }
}
