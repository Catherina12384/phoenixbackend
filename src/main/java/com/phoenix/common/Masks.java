package com.phoenix.common;

/** Helpers so logs never contain full phone numbers. */
public final class Masks {
    private Masks() {}

    public static String phone(String p) {
        if (p == null || p.length() < 8) return "***";
        return p.substring(0, 3) + "*".repeat(p.length() - 7) + p.substring(p.length() - 4);
    }
}
