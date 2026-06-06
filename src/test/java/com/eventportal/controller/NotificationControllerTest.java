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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
}
