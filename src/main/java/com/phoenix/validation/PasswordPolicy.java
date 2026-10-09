package com.phoenix.validation;

import java.util.regex.Pattern;

/** 8-16 printable ASCII characters (no spaces): lowercase, uppercase, digit and a symbol. */
public final class PasswordPolicy {
    public static final String MESSAGE =
            "Password must be 8-16 characters and include an uppercase letter, a lowercase letter, a digit and a symbol, with no spaces";
    private static final Pattern POLICY =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])[\\x21-\\x7E]{8,16}$");

    private PasswordPolicy() {}

    public static boolean isValid(String password) {
        return password != null && POLICY.matcher(password).matches();
    }
}
