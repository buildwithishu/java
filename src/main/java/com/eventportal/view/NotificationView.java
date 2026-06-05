package com.eventportal.view;

import com.eventportal.controller.NotificationController;
import com.eventportal.model.Notification;
import com.eventportal.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

public class NotificationView {
    private final NotificationController notificationController;
    private final User user;

    public NotificationView(NotificationController notificationController, User user) {
        this.notificationController = notificationController;
        this.user = user;
    }

    public Parent build() {
        VBox list = new VBox(12);
        refresh(list);

        Button markAllButton = new Button("Mark all as read");
        markAllButton.setOnAction(event -> markAllAsRead(list));

        VBox content = new VBox(12, markAllButton, list);
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll");
        return scrollPane;
    }

    private void refresh(VBox list) {
        list.getChildren().clear();
        try {
            List<Notification> notifications = notificationController.findByUser(user.id());
            if (notifications.isEmpty()) {
                list.getChildren().add(label("No notifications yet.", "muted-text"));
                return;
            }
            for (Notification notification : notifications) {
                list.getChildren().add(notificationRow(notification, list));
            }
        } catch (SQLException exception) {
            list.getChildren().add(label("Unable to load notifications. Start PostgreSQL and try again.", "error-text"));
        }
    }

    private Parent notificationRow(Notification notification, VBox list) {
        Label title = label(notification.title() + (notification.read() ? "" : " • unread"), "card-title");
        Label message = label(notification.message(), "muted-text");

        VBox text = new VBox(4, title, message);
        HBox.setHgrow(text, Priority.ALWAYS);

        Button readButton = new Button("Mark read");
        readButton.setDisable(notification.read());
        readButton.setOnAction(event -> markAsRead(notification.id(), list));

        HBox row = new HBox(12, text, readButton);
        row.getStyleClass().add("event-card");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14));
        return row;
    }

    private void markAsRead(long notificationId, VBox list) {
        try {
            notificationController.markAsRead(notificationId);
            refresh(list);
        } catch (SQLException exception) {
            list.getChildren().add(label("Unable to update notification.", "error-text"));
        }
    }

    private void markAllAsRead(VBox list) {
        try {
            notificationController.markAllAsRead(user.id());
            refresh(list);
        } catch (SQLException exception) {
            list.getChildren().add(label("Unable to update notifications.", "error-text"));
        }
    }

    private static Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }
}
