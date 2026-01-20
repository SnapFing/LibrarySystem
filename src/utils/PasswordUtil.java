package utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Utility class for secure password hashing and verification.
 * This implementation uses PBKDF2 with HMAC-SHA256 for password hashing,
 * which is a key derivation function with a configurable number of iterations.
 */

public class PasswordUtil {
    private static final int ITERATIONS = 100_000;
    private static final int KEY_LENGTH = 256; // bits
    private static final SecureRandom RANDOM = new SecureRandom();


    // Hash a plaintext password using PBKDF2WithHmacSHA256
    /**
       Hashes a plain text password using PBKDF2 with HMAC-SHA256.
     * @param plainPassword The plain text password to hash
     * @return The hashed password (encoded as ITERATIONS:salt:hash)
     * @throws IllegalArgumentException if password is null or empty
     */

    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        try {
            byte[] salt = new byte[16];
            RANDOM.nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(plainPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();

            String encoded = ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
            return encoded;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }


    // Verify a plaintext password against the stored encoded hash
    /**
     * @param plainPassword The plain text password to verify
     * @param stored The stored encoded hash to compare against
     * @return true if the password matches, false otherwise
     * @throws IllegalArgumentException if any argument is null or empty
     */
    public static boolean verifyPassword(String plainPassword, String stored) {
        if (plainPassword == null || stored == null || stored.isEmpty()) return false;

        try {
            String[] parts = stored.split(":");
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] hash = Base64.getDecoder().decode(parts[2]);

            PBEKeySpec spec = new PBEKeySpec(plainPassword.toCharArray(), salt, iterations, hash.length * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] testHash = skf.generateSecret(spec).getEncoded();

            if (testHash.length != hash.length) return false;
            int diff = 0;
            for (int i = 0; i < hash.length; i++) diff |= hash[i] ^ testHash[i];
            return diff == 0;
        } catch (Exception e) {
            return false;
        }
    }


    // Determine if the stored password needs rehashing (e.g., iterations changed)
    /**
     * @param stored The stored encoded hash to check
     * @return true if the password needs rehashing, false otherwise
     */
    public static boolean needsRehash(String stored) {
        if (stored == null || stored.isEmpty()) return true;
        try {
            String[] parts = stored.split(":");
            int iterations = Integer.parseInt(parts[0]);
            return iterations < ITERATIONS;
        } catch (Exception e) {
            return true;
        }
    }


    // Validate Password Strength
    /**
     * @param password The password to validate
     * @return null if valid, otherwise an error message
     */
    public static String validatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return "Password cannot be empty";
        }

        if (password.length() < 8) {
            return "Password must be at least 8 character long";
        }

        if (password.length() > 72) {   // practical limit
            return "Password must be less than 72 characters";
        }

        if (!password.matches(".*\\d.*")) { // Check for at least one digit
            return "Password must contain at least one digit";
        }
        // Check for at least one letter
        if (!password.matches(".*[a-zA-Z].*")) {
            return "Password must contain at least one letter";
        }

        // Check for special character
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            return "Password must contain at least one special character";
        }

        return null; // Password is valid
    }
}
