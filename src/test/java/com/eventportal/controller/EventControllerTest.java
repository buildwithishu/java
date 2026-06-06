package com.eventportal.controller;

import com.eventportal.dao.EventDAO;
import com.eventportal.model.Event;
import com.eventportal.model.EventCategory;
import com.eventportal.model.EventStatus;
import com.eventportal.model.Role;
import com.eventportal.model.User;
import com.eventportal.util.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {
    @Mock
    private EventDAO eventDAO;

    private final SessionManager sessionManager = SessionManager.getInstance();
    private EventController eventController;

    @BeforeEach
    void setUp() {
        sessionManager.login(admin());
        eventController = new EventController(eventDAO, sessionManager);
    }

    @AfterEach
    void tearDown() {
        sessionManager.logout();
    }

    @Test
    void markOngoing_shouldAdvanceUpcomingEvent() throws SQLException {
        Event upcoming = event(EventStatus.UPCOMING);
        when(eventDAO.findById(upcoming.id())).thenReturn(Optional.of(upcoming));
        when(eventDAO.updateEvent(org.mockito.ArgumentMatchers.any(Event.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Event updated = eventController.markOngoing(upcoming.id());

        assertThat(updated.status()).isEqualTo(EventStatus.ONGOING);
        verify(eventDAO).updateEvent(updated);
    }

    @Test
    void markCompleted_shouldRejectUpcomingEvent() throws SQLException {
        Event upcoming = event(EventStatus.UPCOMING);
        when(eventDAO.findById(upcoming.id())).thenReturn(Optional.of(upcoming));

        assertThatThrownBy(() -> eventController.markCompleted(upcoming.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UPCOMING")
                .hasMessageContaining("COMPLETED");
        verify(eventDAO, never()).updateEvent(org.mockito.ArgumentMatchers.any(Event.class));
    }

    @Test
    void cancelEvent_shouldRejectCompletedEvent() throws SQLException {
        Event completed = event(EventStatus.COMPLETED);
        when(eventDAO.findById(completed.id())).thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> eventController.cancelEvent(completed.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("COMPLETED")
                .hasMessageContaining("CANCELLED");
        verify(eventDAO, never()).updateEvent(org.mockito.ArgumentMatchers.any(Event.class));
    }

    @Test
    void updateEvent_shouldRejectCancelledEvent() throws SQLException {
        Event cancelled = event(EventStatus.CANCELLED);

        assertThatThrownBy(() -> eventController.updateEvent(cancelled))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cancelled events");
        verify(eventDAO, never()).updateEvent(org.mockito.ArgumentMatchers.any(Event.class));
    }

    private static Event event(EventStatus status) {
        OffsetDateTime now = OffsetDateTime.now();
        return new Event(
                11,
                "Architecture Workshop",
                "A practical system design workshop.",
                EventCategory.WORKSHOP,
                "Seminar Hall",
                LocalDate.now().plusDays(5),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                50,
                4,
                status,
                1,
                now,
                now
        );
    }

    private static User admin() {
        return new User(
                1,
                "admin",
                "admin@college.edu",
                "hash",
                Role.ADMIN,
                "Portal Administrator",
                "Administration",
                OffsetDateTime.now()
        );
    }
}
