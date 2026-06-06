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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {
    @Mock
    private RegistrationDAO registrationDAO;
    @Mock
    private EventDAO eventDAO;
    @Mock
    private UserDAO userDAO;
    @Mock
    private NotificationController notificationController;

    private final SessionManager sessionManager = SessionManager.getInstance();
    private RegistrationController registrationController;

    @BeforeEach
    void setUp() {
        sessionManager.logout();
        registrationController = new RegistrationController(
                registrationDAO,
                eventDAO,
                userDAO,
                notificationController,
                sessionManager
        );
    }

    @AfterEach
    void tearDown() {
        sessionManager.logout();
    }

    @Test
    void registerForEvent_shouldPersistAndNotifyStudent() throws SQLException {
        User student = user(7, Role.STUDENT);
        Event event = event(12, EventStatus.UPCOMING, 3, 40);
        Registration registration = registration(21, student.id(), event.id(), RegistrationStatus.CONFIRMED);
        sessionManager.login(student);
        when(eventDAO.findById(event.id())).thenReturn(Optional.of(event));
        when(registrationDAO.isAlreadyRegistered(student.id(), event.id())).thenReturn(false);
        when(registrationDAO.register(student.id(), event.id())).thenReturn(registration);
        when(notificationController.createRegistrationConfirmation(student.id(), event.id(), event.title()))
                .thenReturn(null);

        assertThat(registrationController.registerForEvent(event.id())).isEqualTo(registration);
        verify(registrationDAO).register(student.id(), event.id());
        verify(notificationController).createRegistrationConfirmation(student.id(), event.id(), event.title());
    }

    @Test
    void registerForEvent_shouldRejectUnavailableEventBeforeDuplicateLookup() throws SQLException {
        User student = user(7, Role.STUDENT);
        Event event = event(12, EventStatus.COMPLETED, 3, 40);
        sessionManager.login(student);
        when(eventDAO.findById(event.id())).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> registrationController.registerForEvent(event.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Event is not available for registration");
        verify(registrationDAO, never()).isAlreadyRegistered(student.id(), event.id());
        verify(registrationDAO, never()).register(student.id(), event.id());
        verifyNoInteractions(notificationController);
    }

    @Test
    void registerForEvent_shouldRejectDuplicateRegistration() throws SQLException {
        User student = user(7, Role.STUDENT);
        Event event = event(12, EventStatus.UPCOMING, 3, 40);
        sessionManager.login(student);
        when(eventDAO.findById(event.id())).thenReturn(Optional.of(event));
        when(registrationDAO.isAlreadyRegistered(student.id(), event.id())).thenReturn(true);

        assertThatThrownBy(() -> registrationController.registerForEvent(event.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You are already registered for this event");
        verify(registrationDAO, never()).register(student.id(), event.id());
        verifyNoInteractions(notificationController);
    }

    @Test
    void cancelRegistration_shouldCancelOwnedRegistrationAndNotify() throws SQLException {
        User student = user(7, Role.STUDENT);
        Event event = event(12, EventStatus.UPCOMING, 3, 40);
        Registration registration = registration(21, student.id(), event.id(), RegistrationStatus.CONFIRMED);
        sessionManager.login(student);
        when(registrationDAO.findByUser(student.id())).thenReturn(List.of(registration));
        when(eventDAO.findById(event.id())).thenReturn(Optional.of(event));
        when(registrationDAO.cancelRegistration(registration.id())).thenReturn(true);
        when(notificationController.createCancellationNotice(student.id(), event.id(), event.title()))
                .thenReturn(null);

        assertThat(registrationController.cancelRegistration(registration.id())).isTrue();
        verify(notificationController).createCancellationNotice(student.id(), event.id(), event.title());
    }

    @Test
    void cancelRegistration_shouldRejectRegistrationOwnedByAnotherStudent() throws SQLException {
        User student = user(7, Role.STUDENT);
        Registration anotherStudentsRegistration =
                registration(21, 99, 12, RegistrationStatus.CONFIRMED);
        sessionManager.login(student);
        when(registrationDAO.findByUser(student.id())).thenReturn(List.of());

        assertThatThrownBy(() -> registrationController.cancelRegistration(anotherStudentsRegistration.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Registration not found");
        verify(registrationDAO, never()).cancelRegistration(anotherStudentsRegistration.id());
        verifyNoInteractions(eventDAO, notificationController);
    }

    @Test
    void findRegisteredUsersForEvent_shouldRequireAdminRole() {
        sessionManager.login(user(7, Role.STUDENT));

        assertThatThrownBy(() -> registrationController.findRegisteredUsersForEvent(12))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Admin access is required");
        verifyNoInteractions(userDAO);
    }

    private static Event event(long id, EventStatus status, int registeredCount, int capacity) {
        OffsetDateTime now = OffsetDateTime.now();
        return new Event(
                id,
                "Code Sprint",
                "A programming competition.",
                EventCategory.TECHNICAL,
                "Lab Block",
                LocalDate.now().plusDays(5),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                capacity,
                registeredCount,
                status,
                1,
                now,
                now
        );
    }

    private static Registration registration(
            long id,
            long userId,
            long eventId,
            RegistrationStatus status
    ) {
        return new Registration(id, userId, eventId, OffsetDateTime.now(), "REG-12-7-ABC12345", status);
    }

    private static User user(long id, Role role) {
        return new User(
                id,
                role == Role.ADMIN ? "admin" : "student",
                role == Role.ADMIN ? "admin@college.edu" : "student@college.edu",
                "hash",
                role,
                role == Role.ADMIN ? "Portal Administrator" : "Student User",
                role == Role.ADMIN ? "Administration" : "Computer Science",
                OffsetDateTime.now()
        );
    }
}
