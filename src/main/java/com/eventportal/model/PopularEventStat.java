package com.eventportal.model;

public record PopularEventStat(long eventId, String title, int registeredCount, int capacity) {
    public PopularEventStat {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        if (registeredCount < 0) {
            throw new IllegalArgumentException("registeredCount cannot be negative");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than zero");
        }
    }
}
