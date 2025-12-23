package utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility class for secure password hashing and verification using BCrypt.
 * BCrypt is a password hashing function designed to be slow and resistant to brute-force attacks.
 */

public class PasswordUtil {

    private static final int BRYPT_WORKLOAD = 12;


    // Hash a plaintext password
    /**
       Hashes a plain text password using BCrypt.
     * @param plainPassword The plain text password to hash
     * @return The hashed password (60 characters)
     * @throws IllegalArgumentException if password is null or empty
     */

    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        return  BCrypt.hashpw(plainPassword, BCrypt.gensalt(BRYPT_WORKLOAD));
    }


    // Verify a plaintext password against a hashed password
    /**
     * @param plainPassword The plain text password to verify
     * @param hashedPassword The hashed password to compare against
     * @return true if the password matches, false otherwise
     * @throws IllegalArgumentException if any argument is null or empty
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null ) {
            return false;
        }

        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {  // Invalid hash format
            return false;
        }
    }


    // Check if the password needs rehashing (e.g., if workload has changed)
    /**
     * @param hashedPassword The hashed password to check
     * @return true if the password needs rehashing, false otherwise
     */
    public static boolean needsRehash(String hashedPassword) {
        if (hashedPassword == null || hashedPassword.isEmpty()) {
            return true;
        }

        // Extract the workload from the hash
        // Bcrypt hash format: $2a$[workload]$[salt][hash]
        try {
            String[] parts = hashedPassword.split("\\$");
            if (parts.length < 4) {
                return true;  // Invalid hash format
            }
            int workload = Integer.parseInt(parts[2]);
            return workload < BRYPT_WORKLOAD;
        } catch (Exception e) {
            return true;  // Any error indicates rehash needed
        }
    }

    // Validate Password Strength
    public static String validatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return "Password cannot be empty";
        }

        if (password.length() < 8) {
            return "Password must be at least 8 character long";
        }

        if (password.length() > 72) {   // BCrypt has a max length 0f 72 bytes
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
