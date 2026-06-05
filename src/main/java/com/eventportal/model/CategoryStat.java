package com.eventportal.model;

public record CategoryStat(EventCategory category, long eventCount) {
    public CategoryStat {
        if (category == null) {
            throw new IllegalArgumentException("category is required");
        }
        if (eventCount < 0) {
            throw new IllegalArgumentException("eventCount cannot be negative");
        }
    }
}
