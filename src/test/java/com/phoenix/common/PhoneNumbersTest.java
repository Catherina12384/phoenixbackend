package com.phoenix.common;

import com.phoenix.exception.InvalidPhoneException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhoneNumbersTest {
    @Test void normalisesTenDigits() { assertEquals("+919500288164", PhoneNumbers.normalize("9500288164")); }
    @Test void normalisesWithCountryCode() { assertEquals("+919500288164", PhoneNumbers.normalize("+91 95002-88164")); }
    @Test void normalisesWith91Prefix() { assertEquals("+919500288164", PhoneNumbers.normalize("919500288164")); }
    @Test void normalisesWithLeadingZero() { assertEquals("+919500288164", PhoneNumbers.normalize("09500288164")); }
    @Test void rejectsLandlineStyleStart() { assertThrows(InvalidPhoneException.class, () -> PhoneNumbers.normalize("5500288164")); }
    @Test void rejectsShort() { assertTrue(PhoneNumbers.tryNormalize("95002").isEmpty()); }
    @Test void rejectsLetters() { assertTrue(PhoneNumbers.tryNormalize("95002abcde").isEmpty()); }
    @Test void rejectsNull() { assertTrue(PhoneNumbers.tryNormalize(null).isEmpty()); }
    @Test void masksPhone() { assertEquals("+91******8164", Masks.phone("+919500288164")); }
}
