package com.eventportal.model;

import java.time.OffsetDateTime;
import java.util.Objects;

public record Certificate(
        long id,
        long registrationId,
        String certificatePath,
        OffsetDateTime generatedAt
) {
    public Certificate {
        if (certificatePath == null || certificatePath.isBlank()) {
            throw new IllegalArgumentException("certificatePath is required");
        }
        Objects.requireNonNull(generatedAt, "generatedAt");
    }
}
