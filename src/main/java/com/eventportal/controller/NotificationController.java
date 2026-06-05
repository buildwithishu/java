package com.eventportal.controller;

import com.eventportal.dao.NotificationDAO;
import com.eventportal.model.Notification;

import java.sql.SQLException;
import java.util.List;

public class NotificationController {
    private final NotificationDAO notificationDAO;

    public NotificationController(NotificationDAO notificationDAO) {
        this.notificationDAO = notificationDAO;
    }

    public Notification createNotification(long userId, String title, String message, Long eventId) throws SQLException {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Notification title is required");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Notification message is required");
        }
        return notificationDAO.create(userId, title.trim(), message.trim(), eventId);
    }

    public Notification createRegistrationConfirmation(long userId, long eventId, String eventTitle) throws SQLException {
        return createNotification(
                userId,
                "Registration Confirmed",
                "You are registered for " + eventTitle + ".",
                eventId
        );
    }

    public Notification createCancellationNotice(long userId, long eventId, String eventTitle) throws SQLException {
        return createNotification(
                userId,
                "Registration Cancelled",
                "Your registration for " + eventTitle + " was cancelled.",
                eventId
        );
    }

    public Notification createCertificateReady(long userId, long eventId, String eventTitle) throws SQLException {
        return createNotification(
                userId,
                "Certificate Ready",
                "Your certificate for " + eventTitle + " is ready to download.",
                eventId
        );
    }

    public List<Notification> findByUser(long userId) throws SQLException {
        return notificationDAO.findByUser(userId);
    }

    public boolean markAsRead(long notificationId) throws SQLException {
        return notificationDAO.markAsRead(notificationId);
    }

    public int markAllAsRead(long userId) throws SQLException {
        return notificationDAO.markAllAsRead(userId);
    }

    public int countUnread(long userId) throws SQLException {
        return notificationDAO.countUnread(userId);
    }
}
