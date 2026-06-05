package com.eventportal.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private DateTimeUtil() {
    }

    public static String formatDate(LocalDate date) {
        return date == null ? "" : DATE_FORMATTER.format(date);
    }

    public static String formatTime(LocalTime time) {
        return time == null ? "" : TIME_FORMATTER.format(time);
    }

    public static String formatDateTime(OffsetDateTime dateTime) {
        return dateTime == null ? "" : DATE_TIME_FORMATTER.format(dateTime);
    }
}
