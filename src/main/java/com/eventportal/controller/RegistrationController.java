package com.eventportal.controller;

import com.eventportal.dao.EventDAO;
import com.eventportal.dao.RegistrationDAO;
import com.eventportal.model.Event;
import com.eventportal.model.Registration;
import com.eventportal.model.RegistrationDetail;
import com.eventportal.model.Role;
import com.eventportal.model.User;
import com.eventportal.util.SessionManager;

import java.sql.SQLException;
import java.util.List;

public class RegistrationController {
    private final RegistrationDAO registrationDAO;
    private final EventDAO eventDAO;
    private final NotificationController notificationController;
    private final SessionManager sessionManager;

    public RegistrationController(
            RegistrationDAO registrationDAO,
            EventDAO eventDAO,
            NotificationController notificationController,
            SessionManager sessionManager
    ) {
        this.registrationDAO = registrationDAO;
        this.eventDAO = eventDAO;
        this.notificationController = notificationController;
        this.sessionManager = sessionManager;
    }

    public Registration registerForEvent(long eventId) throws SQLException {
        User student = requireStudent();
        Event event = eventDAO.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        if (!event.hasAvailableSeats()) {
            throw new IllegalArgumentException("Event is not available for registration");
        }
        if (registrationDAO.isAlreadyRegistered(student.id(), eventId)) {
            throw new IllegalArgumentException("You are already registered for this event");
        }

        Registration registration = registrationDAO.register(student.id(), eventId);
        notificationController.createRegistrationConfirmation(student.id(), eventId, event.title());
        return registration;
    }

    public boolean cancelRegistration(long registrationId) throws SQLException {
        User student = requireStudent();
        Registration registration = registrationDAO.findByUser(student.id()).stream()
                .filter(candidate -> candidate.id() == registrationId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Registration not found: " + registrationId));

        Event event = eventDAO.findById(registration.eventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + registration.eventId()));
        boolean cancelled = registrationDAO.cancelRegistration(registrationId);
        if (cancelled) {
            notificationController.createCancellationNotice(student.id(), event.id(), event.title());
        }
        return cancelled;
    }

    public List<Registration> findMyRegistrations() throws SQLException {
        User student = requireStudent();
        return registrationDAO.findByUser(student.id());
    }

    public List<RegistrationDetail> findMyRegistrationDetails() throws SQLException {
        User student = requireStudent();
        return registrationDAO.findDetailsByUser(student.id());
    }

    public List<Registration> findRegistrationsForEvent(long eventId) throws SQLException {
        requireAdmin();
        return registrationDAO.findByEvent(eventId);
    }

    public boolean isAlreadyRegistered(long eventId) throws SQLException {
        User student = requireStudent();
        return registrationDAO.isAlreadyRegistered(student.id(), eventId);
    }

    private User requireStudent() {
        User user = sessionManager.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Login is required"));
        if (user.role() != Role.STUDENT) {
            throw new IllegalStateException("Student access is required");
        }
        return user;
    }

    private User requireAdmin() {
        User user = sessionManager.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Login is required"));
        if (user.role() != Role.ADMIN) {
            throw new IllegalStateException("Admin access is required");
        }
        return user;
    }
}
