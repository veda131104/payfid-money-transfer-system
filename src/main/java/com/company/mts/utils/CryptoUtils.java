package com.company.mts.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CryptoUtils {
    private static final byte[] KEY = "PayFidSuperSecureSecretKey123".getBytes(StandardCharsets.UTF_8);

    public static String encrypt(String value) {
        if (value == null) return null;
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            encrypted[i] = (byte) (bytes[i] ^ KEY[i % KEY.length]);
        }
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static String decrypt(String value) {
        if (value == null) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(value);
            byte[] decrypted = new byte[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                decrypted[i] = (byte) (bytes[i] ^ KEY[i % KEY.length]);
            }
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value; // fallback
        }
    }
}
