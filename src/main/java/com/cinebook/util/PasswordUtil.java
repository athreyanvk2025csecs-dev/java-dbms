package com.cinebook.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * PasswordUtil provides secure SHA-256 password hashing and verification
 * for CineBook user accounts.
 */
public final class PasswordUtil {

    private PasswordUtil() {
        // Utility class
    }

    /**
     * Hashes a plaintext password using SHA-256.
     *
     * @param plainPassword plaintext password
     * @return 64-character lowercase hex string
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 hashing algorithm not available in environment", e);
        }
    }

    /**
     * Verifies if a plaintext password matches a stored SHA-256 hash.
     *
     * @param plainPassword plaintext password to test
     * @param storedHash    expected SHA-256 hex string
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) {
            return false;
        }
        String calculated = hashPassword(plainPassword);
        return calculated.equalsIgnoreCase(storedHash);
    }
}
