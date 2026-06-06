package com.eventportal.controller;

import com.eventportal.dao.NotificationDAO;
import com.eventportal.model.Notification;
import com.eventportal.util.ValidationUtil;

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
        if (!ValidationUtil.hasMaxLength(title.trim(), 160)) {
            throw new IllegalArgumentException("Notification title must be at most 160 characters");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Notification message is required");
        }
        if (!ValidationUtil.hasMaxLength(message.trim(), 5000)) {
            throw new IllegalArgumentException("Notification message must be at most 5000 characters");
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

    public boolean createEventReminder(long userId, long eventId, String message) throws SQLException {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Reminder message is required");
        }
        if (!ValidationUtil.hasMaxLength(message.trim(), 5000)) {
            throw new IllegalArgumentException("Reminder message must be at most 5000 characters");
        }
        return notificationDAO.createIfAbsent(
                userId,
                "Event Reminder",
                message.trim(),
                eventId
        ).isPresent();
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

    public int deleteOldNotifications(int daysOld) throws SQLException {
        if (daysOld < 1) {
            throw new IllegalArgumentException("daysOld must be greater than zero");
        }
        return notificationDAO.deleteOld(daysOld);
    }
}
