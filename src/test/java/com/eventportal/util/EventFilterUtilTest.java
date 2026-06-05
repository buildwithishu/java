package com.eventportal.util;

import com.eventportal.model.Event;
import com.eventportal.model.EventCategory;
import com.eventportal.model.EventStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventFilterUtilTest {
    @Test
    void filter_shouldMatchKeywordAcrossTitleDescriptionAndVenue() {
        List<Event> events = List.of(
                event("Code Sprint", "Programming contest", EventCategory.TECHNICAL, "Lab 1", EventStatus.UPCOMING),
                event("Music Night", "Cultural performances", EventCategory.CULTURAL, "Main Auditorium", EventStatus.UPCOMING)
        );

        assertThat(EventFilterUtil.filter(events, "sprint", null, null)).extracting(Event::title).containsExactly("Code Sprint");
        assertThat(EventFilterUtil.filter(events, "perform", null, null)).extracting(Event::title).containsExactly("Music Night");
        assertThat(EventFilterUtil.filter(events, "auditorium", null, null)).extracting(Event::title).containsExactly("Music Night");
    }

    @Test
    void filter_shouldApplyCategoryAndStatusTogether() {
        List<Event> events = List.of(
                event("Java Workshop", "Hands-on Java", EventCategory.WORKSHOP, "Lab 2", EventStatus.UPCOMING),
                event("Cloud Workshop", "Cloud basics", EventCategory.WORKSHOP, "Lab 3", EventStatus.COMPLETED),
                event("Football Final", "Sports meet", EventCategory.SPORTS, "Ground", EventStatus.UPCOMING)
        );

        assertThat(EventFilterUtil.filter(events, "", EventCategory.WORKSHOP, EventStatus.UPCOMING))
                .extracting(Event::title)
                .containsExactly("Java Workshop");
    }

    private static Event event(
            String title,
            String description,
            EventCategory category,
            String venue,
            EventStatus status
    ) {
        return new Event(
                1,
                title,
                description,
                category,
                venue,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                50,
                0,
                status,
                1,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }
}
