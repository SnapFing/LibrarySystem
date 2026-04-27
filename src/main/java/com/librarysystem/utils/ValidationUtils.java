package com.librarysystem.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Comprehensive validation utility class for all user inputs.
 * Provides centralized validation logic with detailed error messages.
 */
public class ValidationUtils {

    // ==================== EMAIL VALIDATION ====================

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    /**
     * Validates email format.
     * @param email Email to validate
     * @return ValidationResult with success status and error message
     */
    public static ValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return ValidationResult.failure("Email cannot be empty");
        }

        email = email.trim();

        if (email.length() > 100) {
            return ValidationResult.failure("Email is too long (max 100 characters)");
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ValidationResult.failure("Invalid email format (e.g., user@example.com)");
        }

        return ValidationResult.success();
    }

    // ==================== PHONE VALIDATION ====================

    private static final Pattern ZAMBIA_PHONE_PATTERN = Pattern.compile(
            "^(\\+260|0)?[0-9]{9}$"
    );

    private static final Pattern INTERNATIONAL_PHONE_PATTERN = Pattern.compile(
            "^[+]?[0-9]{10,15}$"
    );

    /**
     * Validates phone number (Zambian format).
     * @param phone Phone number to validate
     * @return ValidationResult with success status and error message
     */
    public static ValidationResult validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return ValidationResult.failure("Phone number cannot be empty");
        }

        phone = phone.trim().replaceAll("\\s+", ""); // Remove spaces

        if (!ZAMBIA_PHONE_PATTERN.matcher(phone).matches()) {
            return ValidationResult.failure(
                    "Invalid phone number format (e.g., 0770000000 or +260977123456)"
            );
        }

        return ValidationResult.success();
    }

    /**
     * Validates international phone number.
     */
    public static ValidationResult validateInternationalPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return ValidationResult.failure("Phone number cannot be empty");
        }

        phone = phone.trim().replaceAll("\\s+", "");

        if (!INTERNATIONAL_PHONE_PATTERN.matcher(phone).matches()) {
            return ValidationResult.failure("Invalid phone number format (10-15 digits)");
        }

        return ValidationResult.success();
    }

    // ==================== NAME VALIDATION ====================

    private static final Pattern NAME_PATTERN = Pattern.compile(
            "^[a-zA-Z][a-zA-Z\\s'-]{1,49}$"
    );

    /**
     * Validates person name (first/last name).
     */
    public static ValidationResult validateName(String name, String fieldName) {
        if (name == null || name.trim().isEmpty()) {
            return ValidationResult.failure(fieldName + " cannot be empty");
        }

        name = name.trim();

        if (name.length() < 2) {
            return ValidationResult.failure(fieldName + " must be at least 2 characters");
        }

        if (name.length() > 50) {
            return ValidationResult.failure(fieldName + " is too long (max 50 characters)");
        }

        if (!NAME_PATTERN.matcher(name).matches()) {
            return ValidationResult.failure(
                    fieldName + " can only contain letters, spaces, hyphens, and apostrophes"
            );
        }

        return ValidationResult.success();
    }

    // ==================== ISBN VALIDATION ====================

    /**
     * Validates ISBN-10 or ISBN-13.
     */
    public static ValidationResult validateISBN(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return ValidationResult.failure("ISBN cannot be empty");
        }

        isbn = isbn.trim().replaceAll("[\\s-]", ""); // Remove spaces and hyphens

        if (isbn.length() == 10) {
            return validateISBN10(isbn);
        } else if (isbn.length() == 13) {
            return validateISBN13(isbn);
        } else {
            return ValidationResult.failure("ISBN must be 10 or 13 digits");
        }
    }

    private static ValidationResult validateISBN10(String isbn) {
        if (!isbn.matches("^[0-9]{9}[0-9X]$")) {
            return ValidationResult.failure("Invalid ISBN-10 format");
        }

        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (isbn.charAt(i) - '0') * (10 - i);
        }

        char last = isbn.charAt(9);
        sum += (last == 'X') ? 10 : (last - '0');

        if (sum % 11 != 0) {
            return ValidationResult.failure("Invalid ISBN-10 checksum");
        }

        return ValidationResult.success();
    }

    private static ValidationResult validateISBN13(String isbn) {
        if (!isbn.matches("^(978|979)[0-9]{10}$")) {
            return ValidationResult.failure("Invalid ISBN-13 format (must start with 978 or 979)");
        }

        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = isbn.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }

        int checkDigit = (10 - (sum % 10)) % 10;
        if (checkDigit != (isbn.charAt(12) - '0')) {
            return ValidationResult.failure("Invalid ISBN-13 checksum");
        }

        return ValidationResult.success();
    }

    // ==================== DATE VALIDATION ====================

    /**
     * Validates date string in YYYY-MM-DD format.
     */
    public static ValidationResult validateDate(String dateStr, String fieldName) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return ValidationResult.failure(fieldName + " cannot be empty");
        }

        try {
            LocalDate date = LocalDate.parse(dateStr.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
            return ValidationResult.success();
        } catch (DateTimeParseException e) {
            return ValidationResult.failure(
                    fieldName + " must be in format YYYY-MM-DD (e.g., 2024-12-25)"
            );
        }
    }

    /**
     * Validates date is not in the past.
     */
    public static ValidationResult validateFutureDate(String dateStr, String fieldName) {
        ValidationResult dateValidation = validateDate(dateStr, fieldName);
        if (!dateValidation.isValid()) {
            return dateValidation;
        }

        LocalDate date = LocalDate.parse(dateStr.trim());
        if (date.isBefore(LocalDate.now())) {
            return ValidationResult.failure(fieldName + " cannot be in the past");
        }

        return ValidationResult.success();
    }

    /**
     * Validates date range (start before end).
     */
    public static ValidationResult validateDateRange(String startDate, String endDate) {
        ValidationResult startValidation = validateDate(startDate, "Start date");
        if (!startValidation.isValid()) {
            return startValidation;
        }

        ValidationResult endValidation = validateDate(endDate, "End date");
        if (!endValidation.isValid()) {
            return endValidation;
        }

        LocalDate start = LocalDate.parse(startDate.trim());
        LocalDate end = LocalDate.parse(endDate.trim());

        if (!end.isAfter(start)) {
            return ValidationResult.failure("End date must be after start date");
        }

        return ValidationResult.success();
    }

    // ==================== NUMERIC VALIDATION ====================

    /**
     * Validates integer within range.
     */
    public static ValidationResult validateInteger(String value, String fieldName, int min, int max) {
        if (value == null || value.trim().isEmpty()) {
            return ValidationResult.failure(fieldName + " cannot be empty");
        }

        try {
            int num = Integer.parseInt(value.trim());

            if (num < min || num > max) {
                return ValidationResult.failure(
                        fieldName + " must be between " + min + " and " + max
                );
            }

            return ValidationResult.success();
        } catch (NumberFormatException e) {
            return ValidationResult.failure(fieldName + " must be a valid number");
        }
    }

    /**
     * Validates positive integer.
     */
    public static ValidationResult validatePositiveInteger(String value, String fieldName) {
        return validateInteger(value, fieldName, 1, Integer.MAX_VALUE);
    }

    /**
     * Validates decimal number (for prices, fines, etc.).
     */
    public static ValidationResult validateDecimal(String value, String fieldName, double min, double max) {
        if (value == null || value.trim().isEmpty()) {
            return ValidationResult.failure(fieldName + " cannot be empty");
        }

        try {
            double num = Double.parseDouble(value.trim());

            if (num < min || num > max) {
                return ValidationResult.failure(
                        fieldName + " must be between " + min + " and " + max
                );
            }

            return ValidationResult.success();
        } catch (NumberFormatException e) {
            return ValidationResult.failure(fieldName + " must be a valid decimal number");
        }
    }

    // ==================== TEXT VALIDATION ====================

    /**
     * Validates text length.
     */
    public static ValidationResult validateTextLength(String text, String fieldName, int minLength, int maxLength) {
        if (text == null || text.trim().isEmpty()) {
            return ValidationResult.failure(fieldName + " cannot be empty");
        }

        text = text.trim();

        if (text.length() < minLength) {
            return ValidationResult.failure(
                    fieldName + " must be at least " + minLength + " characters"
            );
        }

        if (text.length() > maxLength) {
            return ValidationResult.failure(
                    fieldName + " cannot exceed " + maxLength + " characters"
            );
        }

        return ValidationResult.success();
    }

    /**
     * Validates text is not empty or whitespace only.
     */
    public static ValidationResult validateNotEmpty(String text, String fieldName) {
        if (text == null || text.trim().isEmpty()) {
            return ValidationResult.failure(fieldName + " cannot be empty");
        }
        return ValidationResult.success();
    }

    // ==================== ALPHANUMERIC VALIDATION ====================

    /**
     * Validates alphanumeric string (letters and numbers only).
     */
    public static ValidationResult validateAlphanumeric(String text, String fieldName) {
        if (text == null || text.trim().isEmpty()) {
            return ValidationResult.failure(fieldName + " cannot be empty");
        }

        if (!text.matches("^[a-zA-Z0-9]+$")) {
            return ValidationResult.failure(fieldName + " can only contain letters and numbers");
        }

        return ValidationResult.success();
    }

    // ==================== USERNAME VALIDATION ====================

    /**
     * Validates username format.
     */
    public static ValidationResult validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return ValidationResult.failure("Username cannot be empty");
        }

        username = username.trim();

        if (username.length() < 3) {
            return ValidationResult.failure("Username must be at least 3 characters");
        }

        if (username.length() > 30) {
            return ValidationResult.failure("Username cannot exceed 30 characters");
        }

        if (!username.matches("^[a-zA-Z0-9_.-]+$")) {
            return ValidationResult.failure(
                    "Username can only contain letters, numbers, dots, hyphens, and underscores"
            );
        }

        if (!Character.isLetter(username.charAt(0))) {
            return ValidationResult.failure("Username must start with a letter");
        }

        return ValidationResult.success();
    }

    // ==================== ADDRESS VALIDATION ====================

    /**
     * Validates address.
     */
    public static ValidationResult validateAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return ValidationResult.failure("Address cannot be empty");
        }

        address = address.trim();

        if (address.length() < 5) {
            return ValidationResult.failure("Address is too short (minimum 5 characters)");
        }

        if (address.length() > 200) {
            return ValidationResult.failure("Address is too long (max 200 characters)");
        }

        return ValidationResult.success();
    }

    // ==================== VALIDATION RESULT CLASS ====================

    /**
     * Result object for validation operations.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public String toString() {
            return valid ? "Valid" : "Invalid: " + errorMessage;
        }
    }
}