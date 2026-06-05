package com.eventportal.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrationDetailTest {
    @Test
    void isActive_shouldReflectConfirmedStatus() {
        assertThat(registrationDetail(RegistrationStatus.CONFIRMED).isActive()).isTrue();
        assertThat(registrationDetail(RegistrationStatus.CANCELLED).isActive()).isFalse();
    }

    @Test
    void constructor_shouldRejectMissingEventTitle() {
        assertThatThrownBy(() -> new RegistrationDetail(
                1,
                1,
                "REG-1",
                RegistrationStatus.CONFIRMED,
                OffsetDateTime.now(),
                "",
                LocalDate.now(),
                "Auditorium"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eventTitle");
    }

    private static RegistrationDetail registrationDetail(RegistrationStatus status) {
        return new RegistrationDetail(
                1,
                1,
                "REG-1",
                status,
                OffsetDateTime.now(),
                "Code Sprint",
                LocalDate.now(),
                "Auditorium"
        );
    }
}
