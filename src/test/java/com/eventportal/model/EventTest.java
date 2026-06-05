package com.eventportal.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventTest {
    @Test
    void hasAvailableSeats_shouldReturnTrueOnlyForUpcomingEventsWithCapacity() {
        Event event = event(EventStatus.UPCOMING, 5, 10);

        assertThat(event.hasAvailableSeats()).isTrue();
        assertThat(event(EventStatus.UPCOMING, 10, 10).hasAvailableSeats()).isFalse();
        assertThat(event(EventStatus.CANCELLED, 5, 10).hasAvailableSeats()).isFalse();
    }

    @Test
    void constructor_shouldRejectInvalidTimeRange() {
        assertThatThrownBy(() -> new Event(
                1,
                "Invalid",
                "Invalid time",
                EventCategory.TECHNICAL,
                "Auditorium",
                LocalDate.now().plusDays(1),
                LocalTime.of(12, 0),
                LocalTime.of(11, 0),
                10,
                0,
                EventStatus.UPCOMING,
                1,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endTime");
    }

    private static Event event(EventStatus status, int registeredCount, int capacity) {
        return new Event(
                1,
                "Code Sprint",
                "Programming contest",
                EventCategory.TECHNICAL,
                "Lab",
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                capacity,
                registeredCount,
                status,
                1,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }
}
