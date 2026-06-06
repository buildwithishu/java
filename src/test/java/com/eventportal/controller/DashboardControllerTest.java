package com.eventportal.controller;

import com.eventportal.dao.EventDAO;
import com.eventportal.dao.RegistrationDAO;
import com.eventportal.dao.UserDAO;
import com.eventportal.model.DashboardSummary;
import com.eventportal.model.Event;
import com.eventportal.model.EventCategory;
import com.eventportal.model.EventStatus;
import com.eventportal.model.MonthlyRegistrationStat;
import com.eventportal.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {
    @Mock
    private EventDAO eventDAO;
    @Mock
    private RegistrationDAO registrationDAO;
    @Mock
    private UserDAO userDAO;

    @Test
    void loadAdminSummary_shouldAggregateCountsCategoriesAndPopularity() throws SQLException {
        List<Event> events = List.of(
                event(1, "Small Workshop", EventCategory.WORKSHOP, 5),
                event(2, "Popular Sprint", EventCategory.TECHNICAL, 30),
                event(3, "Tech Talk", EventCategory.TECHNICAL, 12)
        );
        List<MonthlyRegistrationStat> monthly =
                List.of(new MonthlyRegistrationStat(YearMonth.of(2026, 6), 47));
        when(eventDAO.findAll()).thenReturn(events);
        when(registrationDAO.countConfirmed()).thenReturn(47);
        when(userDAO.countByRole(Role.STUDENT)).thenReturn(19);
        when(registrationDAO.countConfirmedByMonth()).thenReturn(monthly);

        DashboardSummary summary =
                new DashboardController(eventDAO, registrationDAO, userDAO).loadAdminSummary();

        assertThat(summary.totalEvents()).isEqualTo(3);
        assertThat(summary.totalRegistrations()).isEqualTo(47);
        assertThat(summary.activeStudents()).isEqualTo(19);
        assertThat(summary.categoryDistribution())
                .filteredOn(stat -> stat.category() == EventCategory.TECHNICAL)
                .singleElement()
                .extracting(stat -> stat.eventCount())
                .isEqualTo(2L);
        assertThat(summary.categoryDistribution())
                .filteredOn(stat -> stat.category() == EventCategory.SPORTS)
                .singleElement()
                .extracting(stat -> stat.eventCount())
                .isEqualTo(0L);
        assertThat(summary.popularEvents())
                .extracting(stat -> stat.title())
                .containsExactly("Popular Sprint", "Tech Talk", "Small Workshop");
        assertThat(summary.monthlyRegistrations()).containsExactlyElementsOf(monthly);
    }

    private static Event event(long id, String title, EventCategory category, int registeredCount) {
        OffsetDateTime now = OffsetDateTime.now();
        return new Event(
                id,
                title,
                "Event description",
                category,
                "Main Campus",
                LocalDate.now().plusDays(5),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                50,
                registeredCount,
                EventStatus.UPCOMING,
                1,
                now,
                now
        );
    }
}
