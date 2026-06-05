package com.eventportal.util;

import com.eventportal.model.Event;
import com.eventportal.model.EventCategory;
import com.eventportal.model.EventStatus;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class EventFilterUtil {
    private EventFilterUtil() {
    }

    public static List<Event> filter(
            List<Event> events,
            String keyword,
            EventCategory category,
            EventStatus status
    ) {
        Objects.requireNonNull(events, "events");
        String normalizedKeyword = normalize(keyword);

        return events.stream()
                .filter(event -> matchesKeyword(event, normalizedKeyword))
                .filter(event -> category == null || event.category() == category)
                .filter(event -> status == null || event.status() == status)
                .toList();
    }

    private static boolean matchesKeyword(Event event, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }
        return contains(event.title(), keyword)
                || contains(event.description(), keyword)
                || contains(event.venue(), keyword);
    }

    private static boolean contains(String value, String keyword) {
        return normalize(value).contains(keyword);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
