package com.eventportal.model;

import java.time.YearMonth;
import java.util.Objects;

public record MonthlyRegistrationStat(
        YearMonth month,
        long registrationCount
) {
    public MonthlyRegistrationStat {
        Objects.requireNonNull(month, "month");
        if (registrationCount < 0) {
            throw new IllegalArgumentException("registrationCount cannot be negative");
        }
    }
}
