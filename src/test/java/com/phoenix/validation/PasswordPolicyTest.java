package com.phoenix.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {
    @Test void acceptsStrongPassword() { assertTrue(PasswordPolicy.isValid("Abcdef1!")); }
    @Test void acceptsSixteenChars() { assertTrue(PasswordPolicy.isValid("Abcdefghij12345!")); }
    @Test void rejectsTooShort() { assertFalse(PasswordPolicy.isValid("Ab1!xyz")); }
    @Test void rejectsTooLong() { assertFalse(PasswordPolicy.isValid("Abcdefghij123456!")); }
    @Test void rejectsMissingUppercase() { assertFalse(PasswordPolicy.isValid("abcdef1!")); }
    @Test void rejectsMissingLowercase() { assertFalse(PasswordPolicy.isValid("ABCDEF1!")); }
    @Test void rejectsMissingDigit() { assertFalse(PasswordPolicy.isValid("Abcdefg!")); }
    @Test void rejectsMissingSymbol() { assertFalse(PasswordPolicy.isValid("Abcdefg1")); }
    @Test void rejectsSpaces() { assertFalse(PasswordPolicy.isValid("Abcd ef1!")); }
    @Test void rejectsNull() { assertFalse(PasswordPolicy.isValid(null)); }
}
