package com.company.mts.utils;

import java.security.SecureRandom;

public class AccountNumberGenerator {

    private static final SecureRandom random = new SecureRandom();
    private static final int ACCOUNT_NUMBER_LENGTH = 12;

    /**
     * Generate a random 12-digit account number
     * Format: XXXXXXXXXXXX (12 digits)
     */
    public static String generate() {
        StringBuilder accountNumber = new StringBuilder(ACCOUNT_NUMBER_LENGTH);

        // First digit should not be 0
        accountNumber.append(random.nextInt(9) + 1);

        // Remaining 11 digits can be 0-9
        for (int i = 1; i < ACCOUNT_NUMBER_LENGTH; i++) {
            accountNumber.append(random.nextInt(10));
        }

        return accountNumber.toString();
    }

    /**
     * Generate account number with specific prefix
     * Used for testing or special account types
     */
    public static String generateWithPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return generate();
        }

        if (prefix.length() >= ACCOUNT_NUMBER_LENGTH) {
            throw new IllegalArgumentException("Prefix too long");
        }

        StringBuilder accountNumber = new StringBuilder(prefix);
        int remainingDigits = ACCOUNT_NUMBER_LENGTH - prefix.length();

        for (int i = 0; i < remainingDigits; i++) {
            accountNumber.append(random.nextInt(10));
        }

        return accountNumber.toString();
    }

    /**
     * Validate account number format
     */
    public static boolean isValid(String accountNumber) {
        if (accountNumber == null || accountNumber.length() != ACCOUNT_NUMBER_LENGTH) {
            return false;
        }

        return accountNumber.matches("\\d{12}");
    }
}