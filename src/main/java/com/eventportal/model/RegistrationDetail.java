package com.eventportal.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record RegistrationDetail(
        long registrationId,
        long eventId,
        String uniqueRegId,
        RegistrationStatus status,
        OffsetDateTime registrationDate,
        String eventTitle,
        LocalDate eventDate,
        String venue
) {
    public RegistrationDetail {
        if (uniqueRegId == null || uniqueRegId.isBlank()) {
            throw new IllegalArgumentException("uniqueRegId is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (registrationDate == null) {
            throw new IllegalArgumentException("registrationDate is required");
        }
        if (eventTitle == null || eventTitle.isBlank()) {
            throw new IllegalArgumentException("eventTitle is required");
        }
        if (eventDate == null) {
            throw new IllegalArgumentException("eventDate is required");
        }
        if (venue == null || venue.isBlank()) {
            throw new IllegalArgumentException("venue is required");
        }
    }

    public boolean isActive() {
        return status == RegistrationStatus.CONFIRMED;
    }
}
