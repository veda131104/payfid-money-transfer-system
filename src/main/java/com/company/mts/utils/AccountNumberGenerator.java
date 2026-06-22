package com.company.mts.utils;

import java.security.SecureRandom;

public class AccountNumberGenerator {

    private static final SecureRandom random = new SecureRandom();
    // Indian banking standard: account numbers range from 9 to 18 digits
    private static final int ACCOUNT_NUMBER_LENGTH = 12; // default generated length
    private static final int MIN_LENGTH = 9;
    private static final int MAX_LENGTH = 18;

    /**
     * Generate a random 12-digit account number (default generated length)
     * Format: XXXXXXXXXXXX (12 digits, no leading zero)
     * Note: Valid account numbers range from 9-18 digits per Indian banking standards
     */
    public static String generate() {
        StringBuilder accountNumber = new StringBuilder(ACCOUNT_NUMBER_LENGTH);

        // First digit should not be 0
        accountNumber.append(random.nextInt(9) + 1);

        // Remaining digits can be 0-9
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
     * Validate account number format.
     * Accepts 9 to 18 digits — the Indian banking standard range.
     */
    public static boolean isValid(String accountNumber) {
        if (accountNumber == null) {
            return false;
        }
        int len = accountNumber.length();
        if (len < MIN_LENGTH || len > MAX_LENGTH) {
            return false;
        }
        return accountNumber.matches("\\d{" + MIN_LENGTH + "," + MAX_LENGTH + "}");
    }
}
