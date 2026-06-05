package com.eventportal.model;

import java.time.OffsetDateTime;
import java.util.Objects;

public record Registration(
        long id,
        long userId,
        long eventId,
        OffsetDateTime registrationDate,
        String uniqueRegId,
        RegistrationStatus status
) {
    public Registration {
        Objects.requireNonNull(registrationDate, "registrationDate");
        if (uniqueRegId == null || uniqueRegId.isBlank()) {
            throw new IllegalArgumentException("uniqueRegId is required");
        }
        Objects.requireNonNull(status, "status");
    }

    public boolean isActive() {
        return status == RegistrationStatus.CONFIRMED;
    }
}
