package com.eventportal.view;

import com.eventportal.controller.EventController;
import com.eventportal.controller.RegistrationController;
import com.eventportal.model.Event;
import com.eventportal.model.Role;
import com.eventportal.model.User;
import com.eventportal.util.DateTimeUtil;
import com.eventportal.util.FxTaskRunner;
import com.eventportal.util.NavigationManager;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class EventDetailView {
    private final Event event;
    private final EventController eventController;
    private final User currentUser;
    private final RegistrationController registrationController;
    private final NavigationManager navigationManager;

    public EventDetailView(
            Event event,
            EventController eventController,
            User currentUser,
            RegistrationController registrationController,
            NavigationManager navigationManager
    ) {
        this.event = event;
        this.eventController = eventController;
        this.currentUser = currentUser;
        this.registrationController = registrationController;
        this.navigationManager = navigationManager;
    }

    public Parent build() {
        Button backButton = new Button("Back");
        backButton.setOnAction(action -> navigationManager.goBack());

        Label title = new Label(event.title());
        title.getStyleClass().add("screen-title");

        VBox content = new VBox(
                12,
                backButton,
                title,
                detail("Category", event.category().name()),
                detail("Status", event.status().name()),
                detail("Date", DateTimeUtil.formatDate(event.eventDate())),
                detail("Time", DateTimeUtil.formatTime(event.startTime()) + " - " + DateTimeUtil.formatTime(event.endTime())),
                detail("Venue", event.venue()),
                detail("Capacity", event.registeredCount() + " / " + event.capacity()),
                organizerDetail(),
                detail("Description", event.description())
        );

        if (currentUser.role() == Role.STUDENT) {
            content.getChildren().add(studentActionSection());
        }
        if (currentUser.role() == Role.ADMIN) {
            content.getChildren().add(registeredStudentsSection());
        }

        content.getStyleClass().add("content-panel-wide");
        content.setPadding(new Insets(20));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll");

        VBox root = new VBox(scrollPane);
        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(24));
        return root;
    }

    private Parent organizerDetail() {
        VBox container = new VBox(detail("Organizer", "Loading..."));
        FxTaskRunner.run(
                "event-organizer-loader",
                () -> eventController.findOrganizerForEvent(event),
                organizer -> container.getChildren().setAll(detail(
                        "Organizer",
                        organizer.fullName() + " - " + organizer.email()
                )),
                failure -> container.getChildren().setAll(detail("Organizer", "Not available."))
        );
        return container;
    }

    private Parent studentActionSection() {
        VBox section = new VBox(8);
        Label message = label("", "error-text");
        Button actionButton = new Button();
        configureStudentAction(actionButton, message);
        section.getChildren().addAll(actionButton, message);
        return section;
    }

    private void configureStudentAction(Button actionButton, Label message) {
        actionButton.setText("Loading...");
        actionButton.setDisable(true);
        FxTaskRunner.run(
                "registration-status-loader",
                () -> registrationController.isAlreadyRegistered(event.id()),
                registered -> configureLoadedStudentAction(actionButton, message, registered, false),
                failure -> {
                    actionButton.setText("Register");
                    actionButton.setDisable(true);
                    showError(message, "Unable to check registration status. Database unavailable.");
                }
        );
    }

    private void configureLoadedStudentAction(
            Button actionButton,
            Label message,
            boolean registered,
            boolean seatReleased
    ) {
        if (registered) {
            actionButton.setText("Cancel Registration");
            actionButton.setDisable(false);
            actionButton.setOnAction(action -> cancelRegistration(actionButton, message));
            return;
        }
        actionButton.setText("Register");
        actionButton.setDisable(!seatReleased && !event.hasAvailableSeats());
        actionButton.setOnAction(action -> registerForEvent(actionButton, message));
    }

    private void registerForEvent(Button actionButton, Label message) {
        actionButton.setDisable(true);
        FxTaskRunner.run(
                "event-detail-registration",
                () -> registrationController.registerForEvent(event.id()),
                registration -> {
                    showSuccess(message, "Registration confirmed.");
                    configureLoadedStudentAction(actionButton, message, true, false);
                },
                failure -> {
                    actionButton.setDisable(false);
                    showError(message, operationMessage(
                            failure,
                            "Registration failed because the database is unavailable."
                    ));
                }
        );
    }

    private void cancelRegistration(Button actionButton, Label message) {
        actionButton.setDisable(true);
        FxTaskRunner.run(
                "event-detail-cancellation",
                () -> registrationController.cancelMyRegistrationForEvent(event.id()),
                cancelled -> {
                    showSuccess(message, "Registration cancelled.");
                    configureLoadedStudentAction(actionButton, message, false, true);
                },
                failure -> {
                    actionButton.setDisable(false);
                    showError(message, operationMessage(
                            failure,
                            "Cancellation failed because the database is unavailable."
                    ));
                }
        );
    }

    private Parent registeredStudentsSection() {
        VBox section = new VBox(8);
        Label title = new Label("Registered Students");
        title.getStyleClass().add("section-title");
        section.getChildren().addAll(title, label("Loading registered students...", "muted-text"));
        FxTaskRunner.run(
                "registered-student-loader",
                () -> registrationController.findRegisteredUsersForEvent(event.id()),
                students -> renderRegisteredStudents(section, title, students),
                failure -> section.getChildren().setAll(
                        title,
                        label("Unable to load registered students.", "error-text")
                )
        );

        return section;
    }

    private static void renderRegisteredStudents(VBox section, Label title, List<User> students) {
        section.getChildren().setAll(title);
        if (students.isEmpty()) {
            section.getChildren().add(label("No students registered yet.", "muted-text"));
            return;
        }
        for (User student : students) {
            section.getChildren().add(label(student.fullName() + " - " + student.email(), "muted-text"));
        }
    }

    private static Parent detail(String label, String value) {
        VBox box = new VBox(4);
        box.getChildren().add(label(label, "muted-text"));
        box.getChildren().add(label(value, "card-title"));
        return box;
    }

    private static Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        return label;
    }

    private static void showSuccess(Label label, String text) {
        label.getStyleClass().remove("error-text");
        if (!label.getStyleClass().contains("success-text")) {
            label.getStyleClass().add("success-text");
        }
        label.setText(text);
    }

    private static void showError(Label label, String text) {
        label.getStyleClass().remove("success-text");
        if (!label.getStyleClass().contains("error-text")) {
            label.getStyleClass().add("error-text");
        }
        label.setText(text);
    }

    private static String operationMessage(Throwable failure, String fallback) {
        return failure instanceof IllegalArgumentException || failure instanceof IllegalStateException
                ? failure.getMessage()
                : fallback;
    }
}
