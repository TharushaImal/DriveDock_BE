package com.climaxion.drivedock_be.util;

import java.util.regex.Pattern;

public class ValidationUtil {
    // Email validation pattern
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    // Phone number validation (Sri Lankan format)
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[0]{1}[7]{1}[01245678]{1}[0-9]{7}$");

    // Username validation (alphanumeric, 3-50 chars)
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_]{3,50}$");

    // Password validation (at least 8 chars, at least one letter and one number)
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,}$");

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    public static boolean isValidDateTime(String dateTime) {
        if (dateTime == null) return false;
        try {
            java.sql.Timestamp.valueOf(dateTime);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean isValidPositiveNumber(String number) {
        if (number == null) return false;
        try {
            double value = Double.parseDouble(number);
            return value > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String sanitizeInput(String input) {
        if (input == null) return null;
        // Remove any potential HTML/JS injection
        return input.replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&#x27;")
                .replaceAll("/", "&#x2F;");
    }
}
