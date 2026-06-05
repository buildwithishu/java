package com.eventportal.model;

import java.time.OffsetDateTime;
import java.util.Objects;

public record User(
        long id,
        String username,
        String email,
        String passwordHash,
        Role role,
        String fullName,
        String department,
        OffsetDateTime createdAt
) {
    public User {
        requireText(username, "username");
        requireText(email, "email");
        requireText(passwordHash, "passwordHash");
        Objects.requireNonNull(role, "role");
        requireText(fullName, "fullName");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
