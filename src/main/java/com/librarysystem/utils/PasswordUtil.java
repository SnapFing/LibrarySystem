package com.librarysystem.utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility class for bcrypt password hashing and verification.
 */
public class PasswordUtil {

    // work factor (cost) – 12 is secure and still fast on modern hardware
    private static final int BCRYPT_ROUNDS = 12;

    /**
     * Hash a plain-text password using bcrypt.
     * @param plainPassword the password to hash
     * @return the bcrypt hash (starts with $2a$...)
     * @throws IllegalArgumentException if password is null or empty
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        String salt = BCrypt.gensalt(BCRYPT_ROUNDS);
        return BCrypt.hashpw(plainPassword, salt);
    }

    /**
     * Verify a plain-text password against a bcrypt hash.
     * @param plainPassword the password to check
     * @param hashedPassword the stored bcrypt hash
     * @return true if the password matches
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null || hashedPassword.isEmpty()) {
            return false;
        }
        // only accept proper bcrypt hashes (starting with $2a$)
        if (!hashedPassword.startsWith("$2a$")) {
            return false;
        }
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }

    /**
     * Validate password strength requirements.
     * @param password the password to check
     * @return null if valid, otherwise an error message
     */
    public static String validatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return "Password cannot be empty";
        }
        if (password.length() < 8) {
            return "Password must be at least 8 characters long";
        }
        if (password.length() > 72) {
            return "Password must be less than 72 characters";
        }
        if (!password.matches(".*\\d.*")) {
            return "Password must contain at least one digit";
        }
        if (!password.matches(".*[a-zA-Z].*")) {
            return "Password must contain at least one letter";
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            return "Password must contain at least one special character";
        }
        return null; // valid
    }
}