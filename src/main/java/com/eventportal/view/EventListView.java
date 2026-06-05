package com.eventportal.view;

import com.eventportal.controller.RegistrationController;
import com.eventportal.model.Event;
import com.eventportal.model.EventCategory;
import com.eventportal.model.EventStatus;
import com.eventportal.model.User;
import com.eventportal.util.DateTimeUtil;
import com.eventportal.util.EventFilterUtil;
import com.eventportal.util.NavigationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
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

        TextField searchField = new TextField();
        searchField.setPromptText("Search events");

        ComboBox<EventCategory> categoryFilter = new ComboBox<>();
        categoryFilter.setPromptText("All Categories");
        categoryFilter.getItems().setAll(EventCategory.values());

        ComboBox<EventStatus> statusFilter = new ComboBox<>();
        statusFilter.setPromptText("All Statuses");
        statusFilter.getItems().setAll(EventStatus.values());

        Button clearButton = new Button("Clear");

        Runnable refresh = () -> refreshEvents(
                list,
                searchField.getText(),
                categoryFilter.getValue(),
                statusFilter.getValue()
        );

        searchField.textProperty().addListener((observable, oldValue, newValue) -> refresh.run());
        categoryFilter.valueProperty().addListener((observable, oldValue, newValue) -> refresh.run());
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> refresh.run());
        clearButton.setOnAction(action -> {
            searchField.clear();
            categoryFilter.setValue(null);
            statusFilter.setValue(null);
            refresh.run();
        });

        HBox filters = new HBox(10, searchField, categoryFilter, statusFilter, clearButton);
        filters.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        refresh.run();

        ScrollPane scrollPane = new ScrollPane(list);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll");

        VBox root = new VBox(12, filters, scrollPane);
        return root;
    }

    private void refreshEvents(VBox list, String keyword, EventCategory category, EventStatus status) {
        list.getChildren().clear();

        List<Event> filteredEvents = EventFilterUtil.filter(events, keyword, category, status);
        if (filteredEvents.isEmpty()) {
            Label empty = new Label("No events match the current filters.");
            empty.getStyleClass().add("muted-text");
            list.getChildren().add(empty);
        } else {
            for (Event event : filteredEvents) {
                list.getChildren().add(eventCard(event));
            }
        }
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
