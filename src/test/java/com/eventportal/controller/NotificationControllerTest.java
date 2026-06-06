package com.eventportal.controller;

import com.eventportal.dao.NotificationDAO;
import com.eventportal.model.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {
    @Mock
    private NotificationDAO notificationDAO;

    private NotificationController notificationController;

    @BeforeEach
    void setUp() {
        notificationController = new NotificationController(notificationDAO);
    }

    @Test
    void createEventReminder_shouldReportWhetherNotificationWasCreated() throws SQLException {
        Notification notification = new Notification(
                1,
                7,
                "Event Reminder",
                "Event starts tomorrow.",
                false,
                OffsetDateTime.now(),
                3L
        );
        when(notificationDAO.createIfAbsent(7, "Event Reminder", "Event starts tomorrow.", 3))
                .thenReturn(Optional.of(notification));

        assertThat(notificationController.createEventReminder(7, 3, " Event starts tomorrow. ")).isTrue();
    }

    @Test
    void createEventReminder_shouldReturnFalseForDuplicate() throws SQLException {
        when(notificationDAO.createIfAbsent(7, "Event Reminder", "Event starts tomorrow.", 3))
                .thenReturn(Optional.empty());

        assertThat(notificationController.createEventReminder(7, 3, "Event starts tomorrow.")).isFalse();
    }

    @Test
    void createEventReminder_shouldRejectBlankMessage() {
        assertThatThrownBy(() -> notificationController.createEventReminder(7, 3, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reminder message");
        verifyNoInteractions(notificationDAO);
    }

    @Test
    void createNotification_shouldTrimContentAndReturnCreatedNotification() throws SQLException {
        Notification notification = notification("Custom title", "Custom message");
        when(notificationDAO.create(7, "Custom title", "Custom message", 3L)).thenReturn(notification);

        assertThat(notificationController.createNotification(
                7,
                " Custom title ",
                " Custom message ",
                3L
        )).isEqualTo(notification);
    }

    @Test
    void createNotification_shouldRejectMissingContent() {
        assertThatThrownBy(() -> notificationController.createNotification(7, " ", "Message", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Notification title is required");
        assertThatThrownBy(() -> notificationController.createNotification(7, "Title", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Notification message is required");
        verifyNoInteractions(notificationDAO);
    }

    @Test
    void createNotification_shouldRejectOversizedContent() {
        assertThatThrownBy(() -> notificationController.createNotification(
                7,
                "T".repeat(161),
                "Message",
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("160");
        assertThatThrownBy(() -> notificationController.createEventReminder(
                7,
                3,
                "M".repeat(5001)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5000");
        verifyNoInteractions(notificationDAO);
    }

    @Test
    void convenienceNotifications_shouldCreateExpectedMessages() throws SQLException {
        notificationController.createRegistrationConfirmation(7, 3, "Code Sprint");
        notificationController.createCancellationNotice(7, 3, "Code Sprint");
        notificationController.createCertificateReady(7, 3, "Code Sprint");

        verify(notificationDAO).create(
                7,
                "Registration Confirmed",
                "You are registered for Code Sprint.",
                3L
        );
        verify(notificationDAO).create(
                7,
                "Registration Cancelled",
                "Your registration for Code Sprint was cancelled.",
                3L
        );
        verify(notificationDAO).create(
                7,
                "Certificate Ready",
                "Your certificate for Code Sprint is ready to download.",
                3L
        );
    }

    @Test
    void notificationQueries_shouldDelegateToDao() throws SQLException {
        Notification notification = notification("Title", "Message");
        when(notificationDAO.findByUser(7)).thenReturn(List.of(notification));
        when(notificationDAO.markAsRead(11)).thenReturn(true);
        when(notificationDAO.markAllAsRead(7)).thenReturn(2);
        when(notificationDAO.countUnread(7)).thenReturn(4);

        assertThat(notificationController.findByUser(7)).containsExactly(notification);
        assertThat(notificationController.markAsRead(11)).isTrue();
        assertThat(notificationController.markAllAsRead(7)).isEqualTo(2);
        assertThat(notificationController.countUnread(7)).isEqualTo(4);
    }

    @Test
    void deleteOldNotifications_shouldValidateAgeAndDelegate() throws SQLException {
        when(notificationDAO.deleteOld(30)).thenReturn(5);

        assertThat(notificationController.deleteOldNotifications(30)).isEqualTo(5);
        assertThatThrownBy(() -> notificationController.deleteOldNotifications(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
        verify(notificationDAO).deleteOld(30);
    }

    private static Notification notification(String title, String message) {
        return new Notification(
                1,
                7,
                title,
                message,
                false,
                OffsetDateTime.now(),
                3L
        );
    }
}
