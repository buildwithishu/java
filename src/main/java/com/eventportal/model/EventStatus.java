package com.eventportal.model;

public enum EventStatus {
    UPCOMING,
    ONGOING,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(EventStatus nextStatus) {
        if (nextStatus == null) {
            return false;
        }
        return switch (this) {
            case UPCOMING -> nextStatus == ONGOING || nextStatus == CANCELLED;
            case ONGOING -> nextStatus == COMPLETED || nextStatus == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
