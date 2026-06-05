package com.eventportal.view;

import com.eventportal.controller.EventController;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

public class AdminEventManagementView {
    private final EventController eventController;
    private final RegistrationController registrationController;
    private final User currentUser;
    private final NavigationManager navigationManager;

    public AdminEventManagementView(
            EventController eventController,
            RegistrationController registrationController,
            User currentUser,
            NavigationManager navigationManager
    ) {
        this.eventController = eventController;
        this.registrationController = registrationController;
        this.currentUser = currentUser;
        this.navigationManager = navigationManager;
    }

    public Parent build() {
        VBox root = new VBox(18);
        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(24));

        Button backButton = new Button("Back");
        backButton.setOnAction(event -> navigationManager.goBack());

        Label title = new Label("Manage Events");
        title.getStyleClass().add("screen-title");

        HBox header = new HBox(12, backButton, title);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox listContainer = new VBox(12);
        Runnable refresh = () -> refreshEvents(listContainer);

        Parent form = new EventFormView(eventController, refresh).build();
        refreshEvents(listContainer);

        ScrollPane scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll");

        root.getChildren().addAll(header, form, scrollPane);
        return root;
    }

    private void refreshEvents(VBox listContainer) {
        listContainer.getChildren().clear();
        try {
            List<Event> events = eventController.findAllEvents();
            if (events.isEmpty()) {
                Label empty = new Label("No events have been created yet.");
                empty.getStyleClass().add("muted-text");
                listContainer.getChildren().add(empty);
                return;
            }
            for (Event event : events) {
                listContainer.getChildren().add(eventRow(event, listContainer));
            }
        } catch (SQLException exception) {
            Label error = new Label("Unable to load events. Start PostgreSQL and try again.");
            error.getStyleClass().add("error-text");
            listContainer.getChildren().add(error);
        }
    }

    private Parent eventRow(Event event, VBox listContainer) {
        Label title = new Label(event.title());
        title.getStyleClass().add("card-title");

        Label details = new Label(DateTimeUtil.formatDate(event.eventDate()) + " • " + event.venue() + " • " + event.status());
        details.getStyleClass().add("muted-text");

        Label capacity = new Label(event.registeredCount() + " / " + event.capacity() + " registered");
        capacity.getStyleClass().add("muted-text");

        VBox text = new VBox(4, title, details, capacity);
        HBox.setHgrow(text, Priority.ALWAYS);

        Button cancelButton = new Button("Cancel Event");
        cancelButton.setDisable(event.status().name().equals("CANCELLED"));
        cancelButton.setOnAction(action -> cancelEvent(event.id(), listContainer));

        Button ongoingButton = new Button("Start");
        ongoingButton.setDisable(!event.status().name().equals("UPCOMING"));
        ongoingButton.setOnAction(action -> changeStatus(event.id(), "ONGOING", listContainer));

        Button completeButton = new Button("Complete");
        completeButton.setDisable(event.status().name().equals("COMPLETED") || event.status().name().equals("CANCELLED"));
        completeButton.setOnAction(action -> changeStatus(event.id(), "COMPLETED", listContainer));

        Button detailsButton = new Button("Details");
        detailsButton.setOnAction(action -> navigationManager.show(new EventDetailView(
                event,
                currentUser,
                registrationController,
                navigationManager
        ).build()));

        HBox row = new HBox(12, text, detailsButton, ongoingButton, completeButton, cancelButton);
        row.getStyleClass().add("event-card");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14));
        return row;
    }

    private void cancelEvent(long eventId, VBox listContainer) {
        try {
            eventController.cancelEvent(eventId);
            refreshEvents(listContainer);
        } catch (SQLException exception) {
            listContainer.getChildren().add(errorLabel("Unable to cancel event. Database unavailable."));
        } catch (RuntimeException exception) {
            listContainer.getChildren().add(errorLabel(exception.getMessage()));
        }
    }

    private void changeStatus(long eventId, String status, VBox listContainer) {
        try {
            if ("ONGOING".equals(status)) {
                eventController.markOngoing(eventId);
            } else {
                eventController.markCompleted(eventId);
            }
            refreshEvents(listContainer);
        } catch (SQLException exception) {
            listContainer.getChildren().add(errorLabel("Unable to update event. Database unavailable."));
        } catch (RuntimeException exception) {
            listContainer.getChildren().add(errorLabel(exception.getMessage()));
        }
    }

    private static Label errorLabel(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("error-text");
        return label;
    }
}
