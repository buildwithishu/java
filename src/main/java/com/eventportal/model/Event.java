package com.eventportal.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Objects;

public record Event(
        long id,
        String title,
        String description,
        EventCategory category,
        String venue,
        LocalDate eventDate,
        LocalTime startTime,
        LocalTime endTime,
        int capacity,
        int registeredCount,
        EventStatus status,
        long createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public Event {
        requireText(title, "title");
        requireText(description, "description");
        Objects.requireNonNull(category, "category");
        requireText(venue, "venue");
        Objects.requireNonNull(eventDate, "eventDate");
        Objects.requireNonNull(startTime, "startTime");
        Objects.requireNonNull(endTime, "endTime");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than zero");
        }
        if (registeredCount < 0 || registeredCount > capacity) {
            throw new IllegalArgumentException("registeredCount must be between zero and capacity");
        }
    }

    public boolean hasAvailableSeats() {
        return status == EventStatus.UPCOMING && registeredCount < capacity;
    }

    public Event withRegisteredCount(int nextRegisteredCount) {
        return new Event(
                id,
                title,
                description,
                category,
                venue,
                eventDate,
                startTime,
                endTime,
                capacity,
                nextRegisteredCount,
                status,
                createdBy,
                createdAt,
                OffsetDateTime.now()
        );
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
