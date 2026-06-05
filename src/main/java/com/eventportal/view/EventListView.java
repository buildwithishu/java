package com.eventportal.view;

import com.eventportal.controller.RegistrationController;
import com.eventportal.model.Event;
import com.eventportal.model.User;
import com.eventportal.util.DateTimeUtil;
import com.eventportal.util.NavigationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

public class EventListView {
    private final RegistrationController registrationController;
    private final NavigationManager navigationManager;
    private final User currentUser;
    private final List<Event> events;

    public EventListView(
            RegistrationController registrationController,
            NavigationManager navigationManager,
            User currentUser,
            List<Event> events
    ) {
        this.registrationController = registrationController;
        this.navigationManager = navigationManager;
        this.currentUser = currentUser;
        this.events = List.copyOf(events);
    }

    public Parent build() {
        VBox list = new VBox(12);
        list.setPadding(new Insets(4));

        if (events.isEmpty()) {
            Label empty = new Label("No upcoming events are available.");
            empty.getStyleClass().add("muted-text");
            list.getChildren().add(empty);
        } else {
            for (Event event : events) {
                list.getChildren().add(eventCard(event));
            }
        }

        ScrollPane scrollPane = new ScrollPane(list);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll");
        return scrollPane;
    }

    private Parent eventCard(Event event) {
        Label title = new Label(event.title());
        title.getStyleClass().add("card-title");

        Label details = new Label(DateTimeUtil.formatDate(event.eventDate()) + " at " + DateTimeUtil.formatTime(event.startTime()) + " • " + event.venue());
        details.getStyleClass().add("muted-text");

        Label category = new Label(event.category().name() + " • " + event.status().name());
        category.getStyleClass().add("badge");

        double progress = event.capacity() == 0 ? 0 : (double) event.registeredCount() / event.capacity();
        ProgressBar capacityBar = new ProgressBar(progress);
        capacityBar.setMaxWidth(Double.MAX_VALUE);

        Label capacity = new Label(event.registeredCount() + " / " + event.capacity() + " registered");
        capacity.getStyleClass().add("muted-text");

        Label message = new Label();
        message.getStyleClass().add("error-text");

        Button registerButton = new Button("Register");
        registerButton.setDisable(!event.hasAvailableSeats());
        registerButton.setOnAction(action -> register(event, registerButton, message));

        Button detailsButton = new Button("View Details");
        detailsButton.setOnAction(action -> navigationManager.show(new EventDetailView(
                event,
                currentUser,
                registrationController,
                navigationManager
        ).build()));

        HBox footer = new HBox(12, capacity, detailsButton, registerButton);
        footer.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(capacity, Priority.ALWAYS);

        VBox card = new VBox(8, title, details, category, capacityBar, footer, message);
        card.getStyleClass().add("event-card");
        card.setPadding(new Insets(16));
        return card;
    }

    private void register(Event event, Button registerButton, Label message) {
        try {
            registrationController.registerForEvent(event.id());
            message.getStyleClass().remove("error-text");
            if (!message.getStyleClass().contains("success-text")) {
                message.getStyleClass().add("success-text");
            }
            message.setText("Registration confirmed.");
            registerButton.setDisable(true);
        } catch (SQLException exception) {
            showError(message, "Registration failed because the database is unavailable.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError(message, exception.getMessage());
        }
    }

    private static void showError(Label message, String text) {
        message.getStyleClass().remove("success-text");
        if (!message.getStyleClass().contains("error-text")) {
            message.getStyleClass().add("error-text");
        }
        message.setText(text);
    }
}
