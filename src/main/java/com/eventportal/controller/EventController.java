package com.eventportal.controller;

import com.eventportal.dao.EventDAO;
import com.eventportal.dao.RegistrationDAO;
import com.eventportal.dao.UserDAO;
import com.eventportal.model.Event;
import com.eventportal.model.EventCategory;
import com.eventportal.model.EventStatus;
import com.eventportal.model.Registration;
import com.eventportal.model.Role;
import com.eventportal.model.User;
import com.eventportal.util.DateTimeUtil;
import com.eventportal.util.SessionManager;
import com.eventportal.util.ValidationUtil;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

public class EventController {
    private final EventDAO eventDAO;
    private final SessionManager sessionManager;
    private final RegistrationDAO registrationDAO;
    private final NotificationController notificationController;
    private final UserDAO userDAO;

    public EventController(EventDAO eventDAO, SessionManager sessionManager) {
        this(eventDAO, sessionManager, null, null, null);
    }

    public EventController(
            EventDAO eventDAO,
            SessionManager sessionManager,
            RegistrationDAO registrationDAO,
            NotificationController notificationController,
            UserDAO userDAO
    ) {
        this.eventDAO = eventDAO;
        this.sessionManager = sessionManager;
        this.registrationDAO = registrationDAO;
        this.notificationController = notificationController;
        this.userDAO = userDAO;
    }

    public Event createEvent(
            String title,
            String description,
            EventCategory category,
            String venue,
            LocalDate eventDate,
            LocalTime startTime,
            LocalTime endTime,
            int capacity
    ) throws SQLException {
        User admin = requireAdmin();
        validateEventInput(title, description, category, venue, eventDate, startTime, endTime, capacity);

        OffsetDateTime now = OffsetDateTime.now();
        Event event = new Event(
                0,
                title.trim(),
                description.trim(),
                category,
                venue.trim(),
                eventDate,
                startTime,
                endTime,
                capacity,
                0,
                EventStatus.UPCOMING,
                admin.id(),
                now,
                now
        );

        return eventDAO.createEvent(event);
    }

    public Event updateEvent(Event event) throws SQLException {
        requireAdmin();
        if (event.status() == EventStatus.COMPLETED) {
            throw new IllegalArgumentException("Completed events cannot be edited");
        }
        if (event.status() == EventStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancelled events cannot be edited");
        }
        validateEventInput(
                event.title(),
                event.description(),
                event.category(),
                event.venue(),
                event.eventDate(),
                event.startTime(),
                event.endTime(),
                event.capacity()
        );
        return eventDAO.updateEvent(event);
    }

    public boolean cancelEvent(long eventId) throws SQLException {
        requireAdmin();
        Event event = eventDAO.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        requireValidTransition(event.status(), EventStatus.CANCELLED);

        Event cancelled = new Event(
                event.id(),
                event.title(),
                event.description(),
                event.category(),
                event.venue(),
                event.eventDate(),
                event.startTime(),
                event.endTime(),
                event.capacity(),
                event.registeredCount(),
                EventStatus.CANCELLED,
                event.createdBy(),
                event.createdAt(),
                OffsetDateTime.now()
        );

        eventDAO.updateEvent(cancelled);
        notifyEventCancellation(cancelled);
        return true;
    }

    public boolean deleteEvent(long eventId) throws SQLException {
        requireAdmin();
        Event event = eventDAO.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        if (event.registeredCount() > 0) {
            throw new IllegalArgumentException("Events with registrations cannot be deleted");
        }
        return eventDAO.deleteEvent(eventId);
    }

    public Event markOngoing(long eventId) throws SQLException {
        return changeStatus(eventId, EventStatus.ONGOING);
    }

    public Event markCompleted(long eventId) throws SQLException {
        return changeStatus(eventId, EventStatus.COMPLETED);
    }

    public List<Event> findAllEvents() throws SQLException {
        return eventDAO.findAll();
    }

    public List<Event> findUpcomingEvents() throws SQLException {
        return eventDAO.findUpcoming();
    }

    public List<Event> searchEvents(String keyword) throws SQLException {
        if (ValidationUtil.isBlank(keyword)) {
            return eventDAO.findAll();
        }
        return eventDAO.searchByKeyword(keyword.trim());
    }

    public User findOrganizerForEvent(Event event) throws SQLException {
        if (userDAO == null) {
            throw new IllegalStateException("Organizer lookup is not configured");
        }
        return userDAO.findById(event.createdBy())
                .orElseThrow(() -> new IllegalArgumentException("Organizer not found: " + event.createdBy()));
    }

    public int sendEventReminders(long eventId) throws SQLException {
        requireAdmin();
        requireNotificationSupport();
        Event event = eventDAO.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        if (event.status() != EventStatus.UPCOMING) {
            throw new IllegalArgumentException("Reminders can only be sent for upcoming events");
        }

        String message = event.title()
                + " is scheduled for "
                + DateTimeUtil.formatDate(event.eventDate())
                + " at "
                + DateTimeUtil.formatTime(event.startTime())
                + " in "
                + event.venue()
                + ".";
        int remindersCreated = 0;
        for (Registration registration : registrationDAO.findByEvent(eventId)) {
            if (registration.isActive()
                    && notificationController.createEventReminder(registration.userId(), eventId, message)) {
                remindersCreated++;
            }
        }
        return remindersCreated;
    }

    private Event changeStatus(long eventId, EventStatus nextStatus) throws SQLException {
        requireAdmin();
        Event event = eventDAO.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        requireValidTransition(event.status(), nextStatus);
        Event updated = new Event(
                event.id(),
                event.title(),
                event.description(),
                event.category(),
                event.venue(),
                event.eventDate(),
                event.startTime(),
                event.endTime(),
                event.capacity(),
                event.registeredCount(),
                nextStatus,
                event.createdBy(),
                event.createdAt(),
                OffsetDateTime.now()
        );
        return eventDAO.updateEvent(updated);
    }

    private static void requireValidTransition(EventStatus currentStatus, EventStatus nextStatus) {
        if (!currentStatus.canTransitionTo(nextStatus)) {
            throw new IllegalArgumentException(
                    "Event status cannot change from " + currentStatus + " to " + nextStatus
            );
        }
    }

    private void notifyEventCancellation(Event event) throws SQLException {
        if (registrationDAO == null || notificationController == null) {
            return;
        }
        for (Registration registration : registrationDAO.findByEvent(event.id())) {
            if (registration.isActive()) {
                notificationController.createNotification(
                        registration.userId(),
                        "Event Cancelled",
                        event.title() + " has been cancelled.",
                        event.id()
                );
            }
        }
    }

    private void requireNotificationSupport() {
        if (registrationDAO == null || notificationController == null) {
            throw new IllegalStateException("Event notifications are not configured");
        }
    }

    private User requireAdmin() {
        User user = sessionManager.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Login is required"));
        if (user.role() != Role.ADMIN) {
            throw new IllegalStateException("Admin access is required");
        }
        return user;
    }

    private static void validateEventInput(
            String title,
            String description,
            EventCategory category,
            String venue,
            LocalDate eventDate,
            LocalTime startTime,
            LocalTime endTime,
            int capacity
    ) {
        if (ValidationUtil.isBlank(title)) {
            throw new IllegalArgumentException("Event title is required");
        }
        if (!ValidationUtil.hasMaxLength(title.trim(), 160)) {
            throw new IllegalArgumentException("Event title must be at most 160 characters");
        }
        if (ValidationUtil.isBlank(description)) {
            throw new IllegalArgumentException("Event description is required");
        }
        if (!ValidationUtil.hasMaxLength(description.trim(), 5000)) {
            throw new IllegalArgumentException("Event description must be at most 5000 characters");
        }
        if (category == null) {
            throw new IllegalArgumentException("Event category is required");
        }
        if (ValidationUtil.isBlank(venue)) {
            throw new IllegalArgumentException("Venue is required");
        }
        if (!ValidationUtil.hasMaxLength(venue.trim(), 160)) {
            throw new IllegalArgumentException("Venue must be at most 160 characters");
        }
        if (!ValidationUtil.isFutureOrToday(eventDate)) {
            throw new IllegalArgumentException("Event date must be today or later");
        }
        if (!ValidationUtil.isValidTimeRange(startTime, endTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        if (!ValidationUtil.isPositiveCapacity(capacity)) {
            throw new IllegalArgumentException("Capacity must be greater than zero");
        }
    }
}
