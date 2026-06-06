package com.eventportal.view;

import com.eventportal.controller.EventController;
import com.eventportal.controller.RegistrationController;
import com.eventportal.model.Event;
import com.eventportal.model.EventStatus;
import com.eventportal.model.User;
import com.eventportal.util.DateTimeUtil;
import com.eventportal.util.FxTaskRunner;
import com.eventportal.util.NavigationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

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

        VBox formContainer = new VBox();
        VBox listContainer = new VBox(12);
        Runnable refresh = () -> refreshEvents(formContainer, listContainer);

        formContainer.getChildren().setAll(new EventFormView(eventController, refresh).build());
        refreshEvents(formContainer, listContainer);

        ScrollPane scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll");

        root.getChildren().addAll(header, formContainer, scrollPane);
        return root;
    }

    private void refreshEvents(VBox formContainer, VBox listContainer) {
        listContainer.getChildren().clear();
        listContainer.getChildren().add(label("Loading events...", "muted-text"));
        FxTaskRunner.run(
                "admin-event-loader",
                eventController::findAllEvents,
                events -> renderEvents(events, formContainer, listContainer),
                failure -> listContainer.getChildren().setAll(errorLabel(
                        "Unable to load events. Start PostgreSQL and try again."
                ))
        );
    }

    private void renderEvents(List<Event> events, VBox formContainer, VBox listContainer) {
        listContainer.getChildren().clear();
        if (events.isEmpty()) {
            listContainer.getChildren().add(label("No events have been created yet.", "muted-text"));
            return;
        }
        for (Event event : events) {
            listContainer.getChildren().add(eventRow(event, formContainer, listContainer));
        }
    }

    private Parent eventRow(Event event, VBox formContainer, VBox listContainer) {
        Label title = new Label(event.title());
        title.getStyleClass().add("card-title");

        Label details = new Label(DateTimeUtil.formatDate(event.eventDate()) + " • " + event.venue() + " • " + event.status());
        details.getStyleClass().add("muted-text");

        Label capacity = new Label(event.registeredCount() + " / " + event.capacity() + " registered");
        capacity.getStyleClass().add("muted-text");

        VBox text = new VBox(4, title, details, capacity);
        HBox.setHgrow(text, Priority.ALWAYS);

        Button cancelButton = new Button("Cancel Event");
        cancelButton.setDisable(event.status() == EventStatus.CANCELLED || event.status() == EventStatus.COMPLETED);
        cancelButton.setOnAction(action -> cancelEvent(
                event.id(),
                cancelButton,
                formContainer,
                listContainer
        ));

        Button ongoingButton = new Button("Start");
        ongoingButton.setDisable(event.status() != EventStatus.UPCOMING);
        ongoingButton.setOnAction(action -> changeStatus(
                event.id(),
                EventStatus.ONGOING,
                ongoingButton,
                formContainer,
                listContainer
        ));

        Button completeButton = new Button("Complete");
        completeButton.setDisable(event.status() != EventStatus.ONGOING);
        completeButton.setOnAction(action -> changeStatus(
                event.id(),
                EventStatus.COMPLETED,
                completeButton,
                formContainer,
                listContainer
        ));

        Button detailsButton = new Button("Details");
        detailsButton.setOnAction(action -> navigationManager.show(new EventDetailView(
                event,
                eventController,
                currentUser,
                registrationController,
                navigationManager
        ).build()));

        Button editButton = new Button("Edit");
        editButton.setDisable(event.status() == EventStatus.COMPLETED || event.status() == EventStatus.CANCELLED);
        editButton.setOnAction(action -> showEditForm(event, formContainer, listContainer));

        Button deleteButton = new Button("Delete");
        deleteButton.setDisable(event.registeredCount() > 0);
        deleteButton.setOnAction(action -> deleteEvent(
                event.id(),
                deleteButton,
                formContainer,
                listContainer
        ));

        Button reminderButton = new Button("Send Reminder");
        reminderButton.setDisable(event.status() != EventStatus.UPCOMING || event.registeredCount() == 0);
        reminderButton.setOnAction(action -> sendReminders(event.id(), reminderButton, listContainer));

        FlowPane actions = new FlowPane(8, 8);
        actions.getChildren().addAll(
                detailsButton,
                editButton,
                reminderButton,
                ongoingButton,
                completeButton,
                cancelButton,
                deleteButton
        );
        actions.setPrefWrapLength(520);

        HBox row = new HBox(12, text, actions);
        row.getStyleClass().add("event-card");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14));
        return row;
    }

    private void showEditForm(Event event, VBox formContainer, VBox listContainer) {
        Runnable refresh = () -> refreshEvents(formContainer, listContainer);
        formContainer.getChildren().setAll(new EventFormView(eventController, refresh, event).build());
    }

    private void deleteEvent(long eventId, Button source, VBox formContainer, VBox listContainer) {
        runAction(
                "event-deleter",
                source,
                () -> eventController.deleteEvent(eventId),
                deleted -> refreshEvents(formContainer, listContainer),
                "Unable to delete event. Database unavailable.",
                listContainer
        );
    }

    private void sendReminders(long eventId, Button source, VBox listContainer) {
        runAction(
                "event-reminder-sender",
                source,
                () -> eventController.sendEventReminders(eventId),
                remindersCreated -> {
                    String message = remindersCreated == 0
                            ? "Reminders were already sent to all active registrants."
                            : "Sent " + remindersCreated + " event reminder(s).";
                    listContainer.getChildren().add(label(message, "success-text"));
                },
                "Unable to send reminders. Database unavailable.",
                listContainer
        );
    }

    private void cancelEvent(long eventId, Button source, VBox formContainer, VBox listContainer) {
        runAction(
                "event-canceller",
                source,
                () -> eventController.cancelEvent(eventId),
                cancelled -> refreshEvents(formContainer, listContainer),
                "Unable to cancel event. Database unavailable.",
                listContainer
        );
    }

    private void changeStatus(
            long eventId,
            EventStatus status,
            Button source,
            VBox formContainer,
            VBox listContainer
    ) {
        runAction(
                "event-status-updater",
                source,
                () -> status == EventStatus.ONGOING
                        ? eventController.markOngoing(eventId)
                        : eventController.markCompleted(eventId),
                updated -> refreshEvents(formContainer, listContainer),
                "Unable to update event. Database unavailable.",
                listContainer
        );
    }

    private static <T> void runAction(
            String taskName,
            Button source,
            Callable<T> operation,
            Consumer<T> onSuccess,
            String fallbackMessage,
            VBox listContainer
    ) {
        source.setDisable(true);
        FxTaskRunner.run(
                taskName,
                operation,
                result -> {
                    source.setDisable(false);
                    onSuccess.accept(result);
                },
                failure -> {
                    source.setDisable(false);
                    listContainer.getChildren().add(errorLabel(operationMessage(failure, fallbackMessage)));
                }
        );
    }

    private static String operationMessage(Throwable failure, String fallback) {
        return failure instanceof IllegalArgumentException || failure instanceof IllegalStateException
                ? failure.getMessage()
                : fallback;
    }

    private static Label errorLabel(String message) {
        return label(message, "error-text");
    }

    private static Label label(String message, String styleClass) {
        Label label = new Label(message);
        label.getStyleClass().add(styleClass);
        return label;
    }
}
