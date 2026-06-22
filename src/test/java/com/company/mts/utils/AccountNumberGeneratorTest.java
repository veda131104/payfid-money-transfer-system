package com.company.mts.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountNumberGeneratorTest {

    @Test
    void generate_Success() {
        String num = AccountNumberGenerator.generate();
        assertNotNull(num);
        assertEquals(12, num.length());
        assertNotEquals('0', num.charAt(0));
        assertTrue(num.matches("\\d{12}"));
    }

    @Test
    void generateWithPrefix_NullOrEmpty_ReturnsDefault() {
        String num1 = AccountNumberGenerator.generateWithPrefix(null);
        String num2 = AccountNumberGenerator.generateWithPrefix("");

        assertEquals(12, num1.length());
        assertEquals(12, num2.length());
    }

    @Test
    void generateWithPrefix_ValidPrefix_Success() {
        String prefix = "987";
        String num = AccountNumberGenerator.generateWithPrefix(prefix);

        assertNotNull(num);
        assertEquals(12, num.length());
        assertTrue(num.startsWith("987"));
        assertTrue(num.matches("\\d{12}"));
    }

    @Test
    void generateWithPrefix_TooLongPrefix_ThrowsException() {
        String longPrefix = "1234567890123"; // length 13
        assertThrows(IllegalArgumentException.class, () -> {
            AccountNumberGenerator.generateWithPrefix(longPrefix);
        });
    }

    @Test
    void isValid_Null_ReturnsFalse() {
        assertFalse(AccountNumberGenerator.isValid(null));
    }

    @Test
    void isValid_TooShort_ReturnsFalse() {
        assertFalse(AccountNumberGenerator.isValid("12345678")); // length 8
    }

    @Test
    void isValid_TooLong_ReturnsFalse() {
        assertFalse(AccountNumberGenerator.isValid("1234567890123456789")); // length 19
    }

    @Test
    void isValid_NonNumeric_ReturnsFalse() {
        assertFalse(AccountNumberGenerator.isValid("12345678a"));
        assertFalse(AccountNumberGenerator.isValid("abcde1234"));
    }

    @Test
    void isValid_ValidDigits_ReturnsTrue() {
        assertTrue(AccountNumberGenerator.isValid("123456789")); // length 9
        assertTrue(AccountNumberGenerator.isValid("123456789012")); // length 12
        assertTrue(AccountNumberGenerator.isValid("123456789012345678")); // length 18
    }
}
