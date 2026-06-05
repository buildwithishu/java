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
                EventStatus.COMPLETED,
                LocalDate.now(),
                "Auditorium"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eventTitle");
    }

    @Test
    void canGenerateCertificate_shouldRequireConfirmedRegistrationAndCompletedEvent() {
        assertThat(registrationDetail(RegistrationStatus.CONFIRMED, EventStatus.COMPLETED).canGenerateCertificate()).isTrue();
        assertThat(registrationDetail(RegistrationStatus.CONFIRMED, EventStatus.UPCOMING).canGenerateCertificate()).isFalse();
        assertThat(registrationDetail(RegistrationStatus.CANCELLED, EventStatus.COMPLETED).canGenerateCertificate()).isFalse();
    }

    private static RegistrationDetail registrationDetail(RegistrationStatus status) {
        return registrationDetail(status, EventStatus.COMPLETED);
    }

    private static RegistrationDetail registrationDetail(RegistrationStatus status, EventStatus eventStatus) {
        return new RegistrationDetail(
                1,
                1,
                "REG-1",
                status,
                OffsetDateTime.now(),
                "Code Sprint",
                eventStatus,
                LocalDate.now(),
                "Auditorium"
        );
    }
}
