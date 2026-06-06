package com.eventportal.view;

import com.eventportal.controller.NotificationController;
import com.eventportal.model.Notification;
import com.eventportal.model.User;
import com.eventportal.util.DateTimeUtil;
import com.eventportal.util.FxTaskRunner;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

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
        Label unreadSummary = label("", "muted-text");
        refresh(list, unreadSummary);

        Button markAllButton = new Button("Mark all as read");
        markAllButton.setOnAction(event -> markAllAsRead(markAllButton, list, unreadSummary));

        HBox toolbar = new HBox(12, unreadSummary, markAllButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(12, toolbar, list);
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll");
        return scrollPane;
    }

    private void refresh(VBox list, Label unreadSummary) {
        list.getChildren().clear();
        list.getChildren().add(label("Loading notifications...", "muted-text"));
        FxTaskRunner.run(
                "notification-list-loader",
                () -> notificationController.findByUser(user.id()),
                notifications -> renderNotifications(list, unreadSummary, notifications),
                failure -> {
                    list.getChildren().clear();
                    list.getChildren().add(label(
                            "Unable to load notifications. Start PostgreSQL and try again.",
                            "error-text"
                    ));
                }
        );
    }

    private void renderNotifications(VBox list, Label unreadSummary, List<Notification> notifications) {
        list.getChildren().clear();
        updateUnreadSummary(unreadSummary, notifications);
        if (notifications.isEmpty()) {
            list.getChildren().add(label("No notifications yet.", "muted-text"));
            return;
        }
        for (Notification notification : notifications) {
            list.getChildren().add(notificationRow(notification, list, unreadSummary));
        }
    }

    private Parent notificationRow(Notification notification, VBox list, Label unreadSummary) {
        Label title = label(notification.title(), "card-title");
        Label message = label(notification.message(), "muted-text");
        Label status = label(notification.read() ? "Read" : "Unread", notification.read() ? "muted-text" : "badge");
        Label timestamp = label(DateTimeUtil.formatDateTime(notification.createdAt()), "muted-text");

        VBox text = new VBox(4, title, message, timestamp);
        HBox.setHgrow(text, Priority.ALWAYS);

        Button readButton = new Button("Mark read");
        readButton.setDisable(notification.read());
        readButton.setOnAction(event -> markAsRead(notification.id(), readButton, list, unreadSummary));

        HBox row = new HBox(12, text, status, readButton);
        row.getStyleClass().add("event-card");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14));
        return row;
    }

    private void markAsRead(long notificationId, Button readButton, VBox list, Label unreadSummary) {
        readButton.setDisable(true);
        FxTaskRunner.run(
                "notification-reader",
                () -> notificationController.markAsRead(notificationId),
                updated -> refresh(list, unreadSummary),
                failure -> {
                    readButton.setDisable(false);
                    list.getChildren().add(label("Unable to update notification.", "error-text"));
                }
        );
    }

    private void markAllAsRead(Button markAllButton, VBox list, Label unreadSummary) {
        markAllButton.setDisable(true);
        FxTaskRunner.run(
                "notification-bulk-reader",
                () -> notificationController.markAllAsRead(user.id()),
                updated -> {
                    markAllButton.setDisable(false);
                    refresh(list, unreadSummary);
                },
                failure -> {
                    markAllButton.setDisable(false);
                    list.getChildren().add(label("Unable to update notifications.", "error-text"));
                }
        );
    }

    private static void updateUnreadSummary(Label unreadSummary, List<Notification> notifications) {
        long unreadCount = notifications.stream().filter(notification -> !notification.read()).count();
        unreadSummary.setText("Unread notifications: " + unreadCount);
    }

    private static Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        return label;
    }
}
