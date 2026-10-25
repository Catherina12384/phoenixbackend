package com.phoenix.common;

import java.util.Locale;

/** What the user typed in the login box: an email, a valid Indian phone (normalised to +91XXXXXXXXXX) or junk. */
public record LoginIdentifier(Kind kind, String value) {
    public enum Kind { EMAIL, PHONE, INVALID }

    public static LoginIdentifier parse(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.indexOf('@') >= 0) {
            return new LoginIdentifier(Kind.EMAIL, s.toLowerCase(Locale.ROOT));
        }
        return PhoneNumbers.tryNormalize(s)
                .map(p -> new LoginIdentifier(Kind.PHONE, p))
                .orElseGet(() -> new LoginIdentifier(Kind.INVALID, s.toLowerCase(Locale.ROOT)));
    }

    /** Safe for logs and the audit trail: phones are masked and junk input is never echoed. */
    public String masked() {
        return switch (kind) {
            case EMAIL -> value;
            case PHONE -> Masks.phone(value);
            case INVALID -> "invalid-identifier";
        };
    }
}
