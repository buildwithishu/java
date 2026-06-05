package com.eventportal.controller;

import com.eventportal.dao.EventDAO;
import com.eventportal.dao.RegistrationDAO;
import com.eventportal.model.Event;
import com.eventportal.model.EventCategory;
import com.eventportal.model.EventStatus;
import com.eventportal.model.Registration;
import com.eventportal.model.Role;
import com.eventportal.model.User;
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

    public EventController(EventDAO eventDAO, SessionManager sessionManager) {
        this(eventDAO, sessionManager, null, null);
    }

    public EventController(
            EventDAO eventDAO,
            SessionManager sessionManager,
            RegistrationDAO registrationDAO,
            NotificationController notificationController
    ) {
        this.eventDAO = eventDAO;
        this.sessionManager = sessionManager;
        this.registrationDAO = registrationDAO;
        this.notificationController = notificationController;
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

    private Event changeStatus(long eventId, EventStatus nextStatus) throws SQLException {
        requireAdmin();
        Event event = eventDAO.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        if (event.status() == EventStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancelled events cannot change status");
        }
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
        if (ValidationUtil.isBlank(description)) {
            throw new IllegalArgumentException("Event description is required");
        }
        if (category == null) {
            throw new IllegalArgumentException("Event category is required");
        }
        if (ValidationUtil.isBlank(venue)) {
            throw new IllegalArgumentException("Venue is required");
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
