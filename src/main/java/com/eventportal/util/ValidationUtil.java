package com.eventportal.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.regex.Pattern;

public final class ValidationUtil {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Z0-9._-]{3,50}$", Pattern.CASE_INSENSITIVE);
    private static final int MAX_PASSWORD_LENGTH = 72;

    private ValidationUtil() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static boolean isValidEmail(String email) {
        return !isBlank(email) && email.length() <= 120 && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidUsername(String username) {
        return !isBlank(username) && USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8 || password.length() > MAX_PASSWORD_LENGTH) {
            return false;
        }

        boolean hasLetter = false;
        boolean hasDigit = false;

        for (int index = 0; index < password.length(); index++) {
            char current = password.charAt(index);
            hasLetter = hasLetter || Character.isLetter(current);
            hasDigit = hasDigit || Character.isDigit(current);
        }

        return hasLetter && hasDigit;
    }

    public static boolean isPositiveCapacity(int capacity) {
        return capacity > 0;
    }

    public static boolean hasMaxLength(String value, int maximumLength) {
        return value != null && value.length() <= maximumLength;
    }

    public static boolean isFutureOrToday(LocalDate date) {
        return date != null && !date.isBefore(LocalDate.now());
    }

    public static boolean isValidTimeRange(LocalTime startTime, LocalTime endTime) {
        return startTime != null && endTime != null && endTime.isAfter(startTime);
    }
}
