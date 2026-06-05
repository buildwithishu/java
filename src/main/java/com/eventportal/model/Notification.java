package com.eventportal.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.OptionalLong;

public record Notification(
        long id,
        long userId,
        String title,
        String message,
        boolean read,
        OffsetDateTime createdAt,
        Long eventId
) {
    public Notification {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public OptionalLong eventIdValue() {
        return eventId == null ? OptionalLong.empty() : OptionalLong.of(eventId);
    }
}
