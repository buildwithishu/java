package com.eventportal.view;

import com.eventportal.controller.EventController;
import com.eventportal.model.EventCategory;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalTime;

public class EventFormView {
    private final EventController eventController;
    private final Runnable refreshHandler;

    public EventFormView(EventController eventController, Runnable refreshHandler) {
        this.eventController = eventController;
        this.refreshHandler = refreshHandler;
    }

    public Parent build() {
        Label title = new Label("Create Event");
        title.getStyleClass().add("section-title");

        TextField titleField = textField("Title");
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Description");
        descriptionArea.setPrefRowCount(4);

        ComboBox<EventCategory> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll(EventCategory.values());
        categoryBox.setPromptText("Category");
        categoryBox.setMaxWidth(Double.MAX_VALUE);

        TextField venueField = textField("Venue");
        DatePicker datePicker = new DatePicker();
        datePicker.setMaxWidth(Double.MAX_VALUE);

        TextField startTimeField = textField("Start HH:mm");
        TextField endTimeField = textField("End HH:mm");
        Spinner<Integer> capacitySpinner = new Spinner<>(1, 5000, 50);
        capacitySpinner.setEditable(true);
        capacitySpinner.setMaxWidth(Double.MAX_VALUE);

        Label messageLabel = new Label();
        messageLabel.getStyleClass().add("error-text");

        Button saveButton = new Button("Create");
        saveButton.getStyleClass().add("primary-action");
        saveButton.setOnAction(event -> createEvent(
                titleField,
                descriptionArea,
                categoryBox,
                venueField,
                datePicker,
                startTimeField,
                endTimeField,
                capacitySpinner,
                messageLabel
        ));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.add(titleField, 0, 0, 2, 1);
        grid.add(descriptionArea, 0, 1, 2, 1);
        grid.add(categoryBox, 0, 2);
        grid.add(venueField, 1, 2);
        grid.add(datePicker, 0, 3);
        grid.add(capacitySpinner, 1, 3);
        grid.add(startTimeField, 0, 4);
        grid.add(endTimeField, 1, 4);

        VBox form = new VBox(12, title, grid, new HBox(saveButton), messageLabel);
        form.getStyleClass().add("event-card");
        form.setPadding(new Insets(16));
        return form;
    }

    private void createEvent(
            TextField titleField,
            TextArea descriptionArea,
            ComboBox<EventCategory> categoryBox,
            TextField venueField,
            DatePicker datePicker,
            TextField startTimeField,
            TextField endTimeField,
            Spinner<Integer> capacitySpinner,
            Label messageLabel
    ) {
        try {
            eventController.createEvent(
                    titleField.getText(),
                    descriptionArea.getText(),
                    categoryBox.getValue(),
                    venueField.getText(),
                    datePicker.getValue(),
                    LocalTime.parse(startTimeField.getText().trim()),
                    LocalTime.parse(endTimeField.getText().trim()),
                    capacitySpinner.getValue()
            );
            showSuccess(messageLabel, "Event created.");
            titleField.clear();
            descriptionArea.clear();
            venueField.clear();
            datePicker.setValue(null);
            startTimeField.clear();
            endTimeField.clear();
            refreshHandler.run();
        } catch (SQLException exception) {
            showError(messageLabel, "Unable to save event. Start PostgreSQL and try again.");
        } catch (RuntimeException exception) {
            showError(messageLabel, exception.getMessage() == null ? "Invalid event details." : exception.getMessage());
        }
    }

    private static TextField textField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private static void showSuccess(Label label, String message) {
        label.getStyleClass().remove("error-text");
        if (!label.getStyleClass().contains("success-text")) {
            label.getStyleClass().add("success-text");
        }
        label.setText(message);
    }

    private static void showError(Label label, String message) {
        label.getStyleClass().remove("success-text");
        if (!label.getStyleClass().contains("error-text")) {
            label.getStyleClass().add("error-text");
        }
        label.setText(message);
    }
}
