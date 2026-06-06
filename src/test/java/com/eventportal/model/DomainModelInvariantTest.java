package com.eventportal.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainModelInvariantTest {
    private final OffsetDateTime now = OffsetDateTime.now();

    @Test
    void user_shouldRequireIdentityFields() {
        assertThatThrownBy(() -> user("", "student@college.edu", "hash", Role.STUDENT, "Student"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("username is required");
        assertThatThrownBy(() -> user("student", "", "hash", Role.STUDENT, "Student"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email is required");
        assertThatThrownBy(() -> user("student", "student@college.edu", "", Role.STUDENT, "Student"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("passwordHash is required");
        assertThatThrownBy(() -> user("student", "student@college.edu", "hash", null, "Student"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> user("student", "student@college.edu", "hash", Role.STUDENT, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("fullName is required");
    }

    @Test
    void notification_shouldValidateTextAndExposeOptionalEvent() {
        Notification withoutEvent = new Notification(1, 7, "Title", "Message", false, now, null);
        Notification withEvent = new Notification(2, 7, "Title", "Message", true, now, 11L);

        assertThat(withoutEvent.eventIdValue()).isEmpty();
        assertThat(withEvent.eventIdValue()).hasValue(11);
        assertThatThrownBy(() -> new Notification(1, 7, "", "Message", false, now, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title is required");
        assertThatThrownBy(() -> new Notification(1, 7, "Title", " ", false, now, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("message is required");
        assertThatThrownBy(() -> new Notification(1, 7, "Title", "Message", false, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void certificateAndRegistration_shouldRejectInvalidPersistenceData() {
        assertThatThrownBy(() -> new Certificate(1, 2, " ", now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("certificatePath is required");
        assertThatThrownBy(() -> new Certificate(1, 2, "/tmp/file.pdf", null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Registration(1, 7, 11, now, "", RegistrationStatus.CONFIRMED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("uniqueRegId is required");
        assertThatThrownBy(() -> new Registration(1, 7, 11, null, "REG-1", RegistrationStatus.CONFIRMED))
                .isInstanceOf(NullPointerException.class);
        assertThat(new Registration(1, 7, 11, now, "REG-1", RegistrationStatus.CANCELLED).isActive())
                .isFalse();
    }

    @Test
    void dashboardRecords_shouldValidateCountsAndCollections() {
        CategoryStat category = new CategoryStat(EventCategory.TECHNICAL, 2);
        PopularEventStat popular = new PopularEventStat(11, "Code Sprint", 5, 20);
        MonthlyRegistrationStat monthly = new MonthlyRegistrationStat(YearMonth.now(), 3);
        DashboardSummary summary = new DashboardSummary(
                2,
                5,
                10,
                List.of(category),
                List.of(popular),
                List.of(monthly)
        );

        assertThat(summary.categoryDistribution()).containsExactly(category);
        assertThat(summary.popularEvents()).containsExactly(popular);
        assertThat(summary.monthlyRegistrations()).containsExactly(monthly);
        assertThatThrownBy(() -> new CategoryStat(null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("category is required");
        assertThatThrownBy(() -> new CategoryStat(EventCategory.TECHNICAL, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PopularEventStat(1, "", 0, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PopularEventStat(1, "Event", -1, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PopularEventStat(1, "Event", 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DashboardSummary(-1, 0, 0, List.of(), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void event_shouldRejectInvalidCountsAndSupportCountCopy() {
        Event event = event(2, 10);
        Event updated = event.withRegisteredCount(3);

        assertThat(updated.registeredCount()).isEqualTo(3);
        assertThat(updated.updatedAt()).isAfterOrEqualTo(event.updatedAt());
        assertThatThrownBy(() -> event(-1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("registeredCount");
        assertThatThrownBy(() -> event(11, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("registeredCount");
    }

    private User user(String username, String email, String hash, Role role, String fullName) {
        return new User(1, username, email, hash, role, fullName, null, now);
    }

    private Event event(int registeredCount, int capacity) {
        return new Event(
                11,
                "Code Sprint",
                "Programming competition.",
                EventCategory.TECHNICAL,
                "Lab",
                LocalDate.now().plusDays(2),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                capacity,
                registeredCount,
                EventStatus.UPCOMING,
                1,
                now,
                now
        );
    }
}
