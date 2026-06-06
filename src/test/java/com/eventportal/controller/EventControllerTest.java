package com.eventportal.controller;

import com.eventportal.dao.EventDAO;
import com.eventportal.dao.RegistrationDAO;
import com.eventportal.dao.UserDAO;
import com.eventportal.model.Event;
import com.eventportal.model.EventCategory;
import com.eventportal.model.EventStatus;
import com.eventportal.model.Registration;
import com.eventportal.model.RegistrationStatus;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {
    @Mock
    private EventDAO eventDAO;
    @Mock
    private RegistrationDAO registrationDAO;
    @Mock
    private NotificationController notificationController;
    @Mock
    private UserDAO userDAO;

    private final SessionManager sessionManager = SessionManager.getInstance();
    private EventController eventController;

    @BeforeEach
    void setUp() {
        sessionManager.login(admin());
        eventController = new EventController(
                eventDAO,
                sessionManager,
                registrationDAO,
                notificationController,
                userDAO
        );
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

    @Test
    void createEvent_shouldNormalizeInputAndAssignAdmin() throws SQLException {
        when(eventDAO.createEvent(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event created = eventController.createEvent(
                " Architecture Workshop ",
                " Practical system design. ",
                EventCategory.WORKSHOP,
                " Seminar Hall ",
                LocalDate.now().plusDays(5),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                50
        );

        assertThat(created.title()).isEqualTo("Architecture Workshop");
        assertThat(created.description()).isEqualTo("Practical system design.");
        assertThat(created.venue()).isEqualTo("Seminar Hall");
        assertThat(created.createdBy()).isEqualTo(admin().id());
        assertThat(created.status()).isEqualTo(EventStatus.UPCOMING);
        assertThat(created.registeredCount()).isZero();
    }

    @Test
    void createEvent_shouldRejectInvalidInputBeforePersistence() {
        assertThatThrownBy(() -> eventController.createEvent(
                " ",
                "Description",
                EventCategory.WORKSHOP,
                "Seminar Hall",
                LocalDate.now().plusDays(5),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                50
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Event title is required");
        verifyNoInteractions(eventDAO);
    }

    @Test
    void createEvent_shouldRejectOversizedDatabaseFields() {
        assertThatThrownBy(() -> eventController.createEvent(
                "T".repeat(161),
                "Description",
                EventCategory.WORKSHOP,
                "Seminar Hall",
                LocalDate.now().plusDays(5),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                50
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("160");
        assertThatThrownBy(() -> eventController.createEvent(
                "Workshop",
                "D".repeat(5001),
                EventCategory.WORKSHOP,
                "Seminar Hall",
                LocalDate.now().plusDays(5),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                50
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5000");
        verifyNoInteractions(eventDAO);
    }

    @Test
    void createEvent_shouldRequireAdminRole() {
        sessionManager.login(student());

        assertThatThrownBy(() -> eventController.createEvent(
                "Workshop",
                "Description",
                EventCategory.WORKSHOP,
                "Seminar Hall",
                LocalDate.now().plusDays(5),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                50
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("Admin access is required");
        verifyNoInteractions(eventDAO);
    }

    @Test
    void updateEvent_shouldValidateAndPersistEditableEvent() throws SQLException {
        Event upcoming = event(EventStatus.UPCOMING);
        when(eventDAO.updateEvent(upcoming)).thenReturn(upcoming);

        assertThat(eventController.updateEvent(upcoming)).isEqualTo(upcoming);
    }

    @Test
    void cancelEvent_shouldNotifyActiveRegistrants() throws SQLException {
        Event upcoming = event(EventStatus.UPCOMING);
        Registration active = registration(21, 7, upcoming.id(), RegistrationStatus.CONFIRMED);
        Registration cancelled = registration(22, 8, upcoming.id(), RegistrationStatus.CANCELLED);
        when(eventDAO.findById(upcoming.id())).thenReturn(Optional.of(upcoming));
        when(registrationDAO.findByEvent(upcoming.id())).thenReturn(List.of(active, cancelled));

        assertThat(eventController.cancelEvent(upcoming.id())).isTrue();
        verify(eventDAO).updateEvent(any(Event.class));
        verify(notificationController).createNotification(
                active.userId(),
                "Event Cancelled",
                upcoming.title() + " has been cancelled.",
                upcoming.id()
        );
        verify(notificationController, never()).createNotification(
                cancelled.userId(),
                "Event Cancelled",
                upcoming.title() + " has been cancelled.",
                upcoming.id()
        );
    }

    @Test
    void deleteEvent_shouldDeleteEventWithoutRegistrations() throws SQLException {
        Event upcoming = event(EventStatus.UPCOMING, 0);
        when(eventDAO.findById(upcoming.id())).thenReturn(Optional.of(upcoming));
        when(eventDAO.deleteEvent(upcoming.id())).thenReturn(true);

        assertThat(eventController.deleteEvent(upcoming.id())).isTrue();
    }

    @Test
    void deleteEvent_shouldRejectEventWithRegistrations() throws SQLException {
        Event upcoming = event(EventStatus.UPCOMING);
        when(eventDAO.findById(upcoming.id())).thenReturn(Optional.of(upcoming));

        assertThatThrownBy(() -> eventController.deleteEvent(upcoming.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("with registrations");
        verify(eventDAO, never()).deleteEvent(upcoming.id());
    }

    @Test
    void eventQueries_shouldDelegateAndNormalizeSearch() throws SQLException {
        Event upcoming = event(EventStatus.UPCOMING);
        when(eventDAO.findAll()).thenReturn(List.of(upcoming));
        when(eventDAO.findUpcoming()).thenReturn(List.of(upcoming));
        when(eventDAO.searchByKeyword("architecture")).thenReturn(List.of(upcoming));

        assertThat(eventController.findAllEvents()).containsExactly(upcoming);
        assertThat(eventController.findUpcomingEvents()).containsExactly(upcoming);
        assertThat(eventController.searchEvents(" architecture ")).containsExactly(upcoming);
        assertThat(eventController.searchEvents(" ")).containsExactly(upcoming);
        verify(eventDAO).searchByKeyword("architecture");
    }

    @Test
    void findOrganizerForEvent_shouldReturnCreator() throws SQLException {
        Event event = event(EventStatus.UPCOMING);
        User organizer = admin();
        when(userDAO.findById(event.createdBy())).thenReturn(Optional.of(organizer));

        assertThat(eventController.findOrganizerForEvent(event)).isEqualTo(organizer);
    }

    @Test
    void sendEventReminders_shouldNotifyOnlyActiveRegistrations() throws SQLException {
        Event upcoming = event(EventStatus.UPCOMING);
        Registration active = registration(21, 7, upcoming.id(), RegistrationStatus.CONFIRMED);
        Registration cancelled = registration(22, 8, upcoming.id(), RegistrationStatus.CANCELLED);
        when(eventDAO.findById(upcoming.id())).thenReturn(Optional.of(upcoming));
        when(registrationDAO.findByEvent(upcoming.id())).thenReturn(List.of(active, cancelled));
        when(notificationController.createEventReminder(
                active.userId(),
                upcoming.id(),
                "Architecture Workshop is scheduled for "
                        + upcoming.eventDate().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))
                        + " at 10:00 AM in Seminar Hall."
        )).thenReturn(true);

        assertThat(eventController.sendEventReminders(upcoming.id())).isEqualTo(1);
    }

    @Test
    void sendEventReminders_shouldRejectNonUpcomingEvent() throws SQLException {
        Event ongoing = event(EventStatus.ONGOING);
        when(eventDAO.findById(ongoing.id())).thenReturn(Optional.of(ongoing));

        assertThatThrownBy(() -> eventController.sendEventReminders(ongoing.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("upcoming events");
        verifyNoInteractions(registrationDAO, notificationController);
    }

    private static Event event(EventStatus status) {
        return event(status, 4);
    }

    private static Event event(EventStatus status, int registeredCount) {
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
                registeredCount,
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

    private static User student() {
        return new User(
                7,
                "student",
                "student@college.edu",
                "hash",
                Role.STUDENT,
                "Student User",
                "Computer Science",
                OffsetDateTime.now()
        );
    }

    private static Registration registration(
            long id,
            long userId,
            long eventId,
            RegistrationStatus status
    ) {
        return new Registration(id, userId, eventId, OffsetDateTime.now(), "REG-" + id, status);
    }
}
